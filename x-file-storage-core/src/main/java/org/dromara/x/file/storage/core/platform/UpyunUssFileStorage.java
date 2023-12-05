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
import java.util.HashMap;
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
import org.dromara.x.file.storage.core.move.MovePretreatment;

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
    private FileStorageClientFactory<RestManager> clientFactory;

    public UpyunUssFileStorage(UpyunUssConfig config, FileStorageClientFactory<RestManager> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        bucketName = config.getBucketName();
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
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename();
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename();
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

    /**
     * 获取对象的元数据
     */
    public HashMap<String, String> getObjectMetadata(FileInfo fileInfo) {
        HashMap<String, String> params = new HashMap<>();
        params.put(RestManager.PARAMS.CONTENT_TYPE.getValue(), fileInfo.getContentType());
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
