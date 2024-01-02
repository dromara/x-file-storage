package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.upyun.RestManager;
import com.upyun.UpException;
import com.upyun.UpYunUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.UpyunUssConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.upload.CompleteMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.UploadPartPretreatment;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 又拍云 USS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class UpyunUssFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private String bucketName;
    private Integer multipartUploadPartSize;
    private FileStorageClientFactory<RestManager> clientFactory;

    public UpyunUssFileStorage(UpyunUssConfig config, FileStorageClientFactory<RestManager> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        bucketName = config.getBucketName();
        multipartUploadPartSize = config.getMultipartUploadPartSize();
        this.clientFactory = clientFactory;
    }

    public RestManager getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    public String getFileKey(FileInfo fileInfo) {
        return fileInfo.getFilePath(fileInfo);
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getThFilePath(fileInfo);
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);

        RestManager manager = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // 又拍云 USS 的 SDK 使用的是 REST API ，看文档不是区分大小文件的，测试大文件也是流式传输的，边读边传，不会占用大量内存
            try (Response result = manager.writeFile(newFileKey, in, getObjectMetadata(fileInfo))) {
                if (!result.isSuccessful()) {
                    throw new UpException(result.toString());
                }
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                Response thResult = manager.writeFile(
                        newThFileKey, new ByteArrayInputStream(thumbnailBytes), getThObjectMetadata(fileInfo));
                IoUtil.close(thResult);
                if (!thResult.isSuccessful()) {
                    throw new UpException(thResult.toString());
                }
            }

            return true;
        } catch (Exception e) {
            try {
                manager.deleteFile(newFileKey, null).close();
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return new MultipartUploadSupportInfo(true, false, false, null);
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        //        Check.initiateMultipartUploadRequireFileSize(platform,fileInfo);
        RestManager manager = getClient();
        try {
            Map<String, String> params = new HashMap<>();
            // X-Upyun-Multi-Disorder	是	String
            // X-Upyun-Multi-Stage	是	String
            // X-Upyun-Multi-Length	是	String	待上传文件的大小，单位 Byte
            // Content-Length	是	String	请求的内容长度
            // Content-MD5	否	String	请求的 MD5 值，需要服务端进行 MD5 校验请填写，等效于签名认证中的 Content-MD5
            // X-Upyun-Multi-Part-Size	否	String	1M整数倍，默认1M，最大50M，单位 Byte
            // X-Upyun-Multi-Type	否	String	待上传文件的 MIME 类型，默认 application/octet-stream，建议自行设置
            // X-Upyun-Meta-X	否	String	给文件添加的元信息，详见 Metadata
            // X-Upyun-Meta-Ttl	否	String	指定文件的生存时间，过期后自动删除，单位天，最大支持 180 天
            params.put("X-Upyun-Multi-Disorder", "true"); // 值为 true, 表示并行式断点续传
            params.put("X-Upyun-Multi-Stage", "initiate"); // 值为 initiate
            if (multipartUploadPartSize != null) {
                params.put("X-Upyun-Multi-Part-Size", String.valueOf(multipartUploadPartSize));
            }
            params.putAll(getObjectMetadata(fileInfo));
            Response result = checkResponse(manager.writeFile(newFileKey, new byte[0], params));
            String uploadId = result.header("X-Upyun-Multi-Uuid");
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        RestManager manager = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();

        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // X-Upyun-Multi-Stage	是	String	值为 upload
            // X-Upyun-Multi-Uuid	是	String	任务标识，初始化时生成
            // X-Upyun-Part-Id	是	String	分块序号，序号从 0 开始
            // Content-Length	是	String	请求的内容长度
            // Content-MD5	否	String	请求的 MD5 值，需要服务端进行 MD5 校验请填写，等效于签名认证中的 Content-MD5

            // 又拍云 USS 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);

            Map<String, String> params = new HashMap<>();
            params.put("X-Upyun-Multi-Stage", "upload"); // 值为 upload
            params.put("X-Upyun-Multi-Uuid", fileInfo.getUploadId()); // 任务标识，初始化时生成
            params.put("X-Upyun-Part-Id", String.valueOf(pre.getPartNumber() - 1)); // 分块序号，序号从 0 开始
            params.put("Content-Length", String.valueOf(partSize)); // 请求的内容长度

            try {
                checkResponse(manager.writeFile(newFileKey, in, params));
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("wrong content-length header")) {
                    throw new FileStorageRuntimeException(
                            "当前上传的分片大小与文件初始化时提供的分片大小不同，又拍云 USS 比较特殊，必须提前传入分片大小（最后一个分片可以小于此大小，但不能超过），"
                                    + "你可以在初始化文件时使用 putMetadata(\"X-Upyun-Multi-Part-Size\", \"1048576\") 方法传入分片大小"
                                    + "或修改配置文件 multipartUploadPartSize 参数，单位字节，最小 1MB，最大 50MB，必须是 1MB 的整数倍，"
                                    + "默认为 "
                                    + multipartUploadPartSize,
                            e);
                }
                throw e;
            }

            fileInfo.setUploadId(fileInfo.getUploadId());
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag("暂无");
            filePartInfo.setPartNumber(pre.getPartNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        RestManager manager = getClient();

        try {
            Map<String, String> params = new HashMap<>();
            params.put("X-Upyun-Multi-Stage", "complete"); // 值为 complete
            params.put("X-Upyun-Multi-Uuid", fileInfo.getUploadId()); // 任务标识，初始化时生成

            Long fileSize = fileInfo.getSize();
            ProgressListener.quickStart(pre.getProgressListener(), fileSize);
            try {
                Response result = checkResponse(manager.writeFile(newFileKey, new byte[0], params));
                ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
                String length = result.header("X-Upyun-Multi-Length");
                if (fileSize == null && length != null) fileInfo.setSize(Long.parseLong(length));
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("invalid x-upyun-part-size")) {
                    throw new FileStorageRuntimeException(
                            "已上传的分片大小与文件初始化时提供的分片大小不同，又拍云 USS 比较特殊，必须提前传入分片大小（最后一个分片可以小于此大小，但不能超过），"
                                    + "你可以在初始化文件时使用 putMetadata(\"X-Upyun-Multi-Part-Size\", \"1048576\") 方法传入分片大小"
                                    + "或修改配置文件 multipartUploadPartSize 参数，单位字节，最小 1MB，最大 50MB，必须是 1MB 的整数倍，"
                                    + "默认为 "
                                    + multipartUploadPartSize,
                            e);
                }
                throw e;
            }

        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 获取对象的元数据
     */
    public HashMap<String, String> getObjectMetadata(FileInfo fileInfo) {
        HashMap<String, String> params = new HashMap<>();
        if (fileInfo.getContentType() != null) {
            params.put(RestManager.PARAMS.CONTENT_TYPE.getValue(), fileInfo.getContentType());
        }
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            params.putAll(fileInfo.getMetadata());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata())) {
            fileInfo.getUserMetadata()
                    .forEach((key, value) ->
                            params.put(key.startsWith("x-upyun-meta-") ? key : ("x-upyun-meta-" + key), value));
        }
        return params;
    }

    /**
     * 获取缩略图对象的元数据
     */
    public HashMap<String, String> getThObjectMetadata(FileInfo fileInfo) {
        HashMap<String, String> params = new HashMap<>();
        params.put(RestManager.PARAMS.CONTENT_TYPE.getValue(), fileInfo.getThContentType());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            params.putAll(fileInfo.getThMetadata());
        }
        if (CollUtil.isNotEmpty(fileInfo.getThUserMetadata())) {
            fileInfo.getThUserMetadata()
                    .forEach((key, value) ->
                            params.put(key.startsWith("x-upyun-meta-") ? key : ("x-upyun-meta-" + key), value));
        }
        return params;
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        RestManager manager = getClient();
        String file = getFileKey(fileInfo);
        String thFile = getThFileKey(fileInfo);

        try (Response ignored = fileInfo.getThFilename() != null ? manager.deleteFile(thFile, null) : null;
                Response ignored2 = manager.deleteFile(file, null)) {
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return exists(getFileKey(fileInfo));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    public boolean exists(String fileKey) throws UpException, IOException {
        Response response = checkResponse(getClient().getFileInfo(fileKey));
        return StrUtil.isNotBlank(response.header("x-upyun-file-size"));
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (Response response = getClient().readFile(getFileKey(fileInfo));
                ResponseBody body = response.body();
                InputStream in = body == null ? null : body.byteStream()) {
            if (body == null) {
                throw new NullPointerException("body is null");
            }
            if (!response.isSuccessful()) {
                throw new UpException(IoUtil.read(in, StandardCharsets.UTF_8));
            }
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (Response response = getClient().readFile(getThFileKey(fileInfo));
                ResponseBody body = response.body();
                InputStream in = body == null ? null : body.byteStream()) {
            if (body == null) {
                throw new NullPointerException("body is null");
            }
            if (!response.isSuccessful()) {
                throw new UpException(IoUtil.read(in, StandardCharsets.UTF_8));
            }
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    public Response checkResponse(Response response) throws UpException, IOException {
        if (!response.isSuccessful()) {
            if (response.body() != null) {
                throw new UpException(response.body().string());
            } else {
                response.close();
                throw new UpException(response.toString());
            }
        }
        response.close();
        return response;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        RestManager client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        long srcFileSize;
        try {
            Response response = checkResponse(client.getFileInfo(srcFileKey));
            srcFileSize = Long.parseLong(
                    Objects.requireNonNull(response.header(RestManager.PARAMS.X_UPYUN_FILE_SIZE.getValue())));
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                checkResponse(client.copyFile(
                        destThFileKey, UpYunUtils.formatPath(bucketName, getThFileKey(srcFileInfo)), null));
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFileSize);
            checkResponse(client.copyFile(destFileKey, UpYunUtils.formatPath(bucketName, srcFileKey), null));
            ProgressListener.quickFinish(pre.getProgressListener(), srcFileSize);
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    IoUtil.close(client.deleteFile(destThFileKey, null));
                } catch (Exception ignored) {
                }
            try {
                IoUtil.close(client.deleteFile(destFileKey, null));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        RestManager client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        long srcFileSize;
        try {
            Response response = checkResponse(client.getFileInfo(srcFileKey));
            srcFileSize = Long.parseLong(
                    Objects.requireNonNull(response.header(RestManager.PARAMS.X_UPYUN_FILE_SIZE.getValue())));
        } catch (Exception e) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 移动缩略图文件
        String srcThFileKey = null;
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            srcThFileKey = getThFileKey(srcFileInfo);
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                checkResponse(client.moveFile(destThFileKey, UpYunUtils.formatPath(bucketName, srcThFileKey), null));
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFileSize);
            checkResponse(client.moveFile(destFileKey, UpYunUtils.formatPath(bucketName, srcFileKey), null));
            ProgressListener.quickFinish(pre.getProgressListener(), srcFileSize);
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    IoUtil.close(client.moveFile(srcThFileKey, UpYunUtils.formatPath(bucketName, destThFileKey), null));
                } catch (Exception ignored) {
                }
            try {
                if (exists(srcFileKey)) {
                    IoUtil.close(client.deleteFile(destFileKey, null));
                } else {
                    IoUtil.close(client.moveFile(srcFileKey, UpYunUtils.formatPath(bucketName, destFileKey), null));
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
