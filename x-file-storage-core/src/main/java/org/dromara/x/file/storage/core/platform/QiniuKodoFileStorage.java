package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.*;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.StringMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.QiniuKodoConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.QiniuKodoFileStorageClientFactory.QiniuKodoClient;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;

/**
 * 七牛云 Kodo 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class QiniuKodoFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private FileStorageClientFactory<QiniuKodoClient> clientFactory;

    public QiniuKodoFileStorage(QiniuKodoConfig config, FileStorageClientFactory<QiniuKodoClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        this.clientFactory = clientFactory;
    }

    public QiniuKodoClient getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);

        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // 七牛云 Kodo 的 SDK 内部会自动分片上传
            QiniuKodoClient client = getClient();
            UploadManager uploadManager = client.getUploadManager();
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            uploadManager.put(in, newFileKey, token, getObjectMetadata(fileInfo), fileInfo.getContentType());
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                String thToken = client.getAuth().uploadToken(bucketName, newThFileKey);
                fileInfo.setThUrl(domain + newThFileKey);
                uploadManager.put(
                        new ByteArrayInputStream(thumbnailBytes),
                        newThFileKey,
                        thToken,
                        getThObjectMetadata(fileInfo),
                        fileInfo.getThContentType());
            }

            return true;
        } catch (Exception e) {
            try {
                getClient().getBucketManager().delete(bucketName, newFileKey);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll();
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        QiniuKodoClient client = getClient();

        try {
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            QiniuKodoClient.UploadActionResult<ApiUploadV2InitUpload.Response> result = client.retryUploadAction(
                    host -> {
                        ApiUploadV2InitUpload api = new ApiUploadV2InitUpload(client.getClient());
                        ApiUploadV2InitUpload.Request request =
                                new ApiUploadV2InitUpload.Request(host, token).setKey(newFileKey);
                        ApiUploadV2InitUpload.Response response = api.request(request);
                        return new QiniuKodoClient.UploadActionResult<>(response.getResponse(), response);
                    },
                    token);
            fileInfo.setUploadId(result.getData().getUploadId());
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        QiniuKodoClient client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            QiniuKodoClient.UploadActionResult<ApiUploadV2UploadPart.Response> result = client.retryUploadAction(
                    host -> {
                        ApiUploadV2UploadPart api = new ApiUploadV2UploadPart(client.getClient());
                        ApiUploadV2UploadPart.Request request = new ApiUploadV2UploadPart.Request(
                                        host, token, fileInfo.getUploadId(), pre.getPartNumber())
                                .setKey(newFileKey)
                                .setUploadData(in, null, -1);
                        ApiUploadV2UploadPart.Response response = api.request(request);
                        return new QiniuKodoClient.UploadActionResult<>(response.getResponse(), response);
                    },
                    token);
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(result.getData().getEtag());
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
        QiniuKodoClient client = getClient();
        try {
            List<Map<String, Object>> partsInfo = pre.getPartInfoList().stream()
                    .map(part -> {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("partNumber", part.getPartNumber());
                        map.put("etag", part.getETag());
                        return map;
                    })
                    .collect(Collectors.toList());
            StringMap metadata = getObjectMetadata(fileInfo);
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            client.retryUploadAction(
                    host -> {
                        ApiUploadV2CompleteUpload api = new ApiUploadV2CompleteUpload(client.getClient());
                        ApiUploadV2CompleteUpload.Request request = new ApiUploadV2CompleteUpload.Request(
                                        host, token, fileInfo.getUploadId(), partsInfo)
                                .setKey(newFileKey)
                                .setFileMimeType(fileInfo.getContentType())
                                .setFileName(null)
                                .setCustomParam(metadata.map())
                                .setCustomMetaParam(metadata.map());
                        ApiUploadV2CompleteUpload.Response response = api.request(request);
                        return new QiniuKodoClient.UploadActionResult<>(response.getResponse(), response);
                    },
                    token);

            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        QiniuKodoClient client = getClient();
        try {
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            client.retryUploadAction(
                    host -> {
                        ApiUploadV2AbortUpload api = new ApiUploadV2AbortUpload(client.getClient());
                        ApiUploadV2AbortUpload.Request request = new ApiUploadV2AbortUpload.Request(
                                        host, token, fileInfo.getUploadId())
                                .setKey(newFileKey);
                        ApiUploadV2AbortUpload.Response response = api.request(request);
                        return new QiniuKodoClient.UploadActionResult<>(response.getResponse(), response);
                    },
                    token);
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        QiniuKodoClient client = getClient();
        try {
            String token = client.getAuth().uploadToken(bucketName, newFileKey);
            QiniuKodoClient.UploadActionResult<ApiUploadV2ListParts.Response> result = client.retryUploadAction(
                    host -> {
                        ApiUploadV2ListParts api = new ApiUploadV2ListParts(client.getClient());
                        ApiUploadV2ListParts.Request request = new ApiUploadV2ListParts.Request(
                                        host, token, fileInfo.getUploadId())
                                .setKey(newFileKey)
                                .setMaxParts(pre.getMaxParts())
                                .setPartNumberMarker(pre.getPartNumberMarker());
                        ApiUploadV2ListParts.Response response = api.request(request);
                        return new QiniuKodoClient.UploadActionResult<>(response.getResponse(), response);
                    },
                    token);
            ApiUploadV2ListParts.Response response = result.getData();
            List<?> parts = response.getParts();
            if (parts == null) parts = Collections.emptyList();
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(parts.stream()
                    .map(p -> {
                        Dict dict = new Dict(BeanUtil.beanToMap(p));
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(dict.getStr("etag"));
                        filePartInfo.setPartNumber(dict.getInt("partNumber"));
                        filePartInfo.setPartSize(dict.getLong("size"));
                        filePartInfo.setLastModified(new Date(dict.getLong("putTime") * 1000));
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));
            list.setMaxParts(pre.getMaxParts());
            list.setIsTruncated(response.getPartNumberMarker() > 0);
            list.setPartNumberMarker(pre.getPartNumberMarker());
            list.setNextPartNumberMarker(response.getPartNumberMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listParts(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        BucketManager manager = getClient().getBucketManager();
        try {
            FileListing result = manager.listFilesV2(
                    bucketName,
                    basePath + pre.getPath() + pre.getFilenamePrefix(),
                    pre.getMarker(),
                    pre.getMaxFiles(),
                    "/");
            ListFilesResult list = new ListFilesResult();
            list.setDirList(Arrays.stream(result.commonPrefixes)
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(Arrays.stream(result.items)
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.key));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.fsize);
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setContentType(item.mimeType);
                        info.setContentMd5(item.md5);
                        info.setLastModified(new Date(item.putTime / 10000));
                        HashMap<String, Object> metadata = new HashMap<>();
                        metadata.put(Constant.Metadata.CONTENT_LENGTH, item.fsize);
                        if (item.mimeType != null) metadata.put(Constant.Metadata.CONTENT_TYPE, item.mimeType);
                        if (item.md5 != null) metadata.put(Constant.Metadata.CONTENT_MD5, item.md5);
                        metadata.put(Constant.Metadata.LAST_MODIFIED, info.getLastModified());
                        if (item.expiration != null)
                            metadata.put(
                                    Constant.Metadata.EXPIRES,
                                    DateUtil.formatHttpDate(new Date(item.expiration * 1000)));
                        info.setMetadata(metadata);
                        info.setUserMetadata(item.meta);
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(pre.getMaxFiles());
            list.setIsTruncated(!result.isEOF());
            list.setMarker(pre.getMarker());
            list.setNextMarker(result.marker);
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        QiniuKodoClient client = getClient();
        try {
            com.qiniu.storage.model.FileInfo file;
            try {
                file = client.getBucketManager().stat(bucketName, fileKey);
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.key));
            info.setUrl(domain + fileKey);
            info.setSize(file.fsize);
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setContentType(file.mimeType);
            info.setContentMd5(file.md5);
            info.setLastModified(new Date(file.putTime / 10000));
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put(Constant.Metadata.CONTENT_LENGTH, file.fsize);
            if (file.mimeType != null) metadata.put(Constant.Metadata.CONTENT_TYPE, file.mimeType);
            if (file.md5 != null) metadata.put(Constant.Metadata.CONTENT_MD5, file.md5);
            metadata.put(Constant.Metadata.LAST_MODIFIED, info.getLastModified());
            if (file.expiration != null)
                metadata.put(Constant.Metadata.EXPIRES, DateUtil.formatHttpDate(new Date(file.expiration * 1000)));
            info.setMetadata(metadata);
            info.setUserMetadata(file.meta);
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取对象的元数据
     */
    public StringMap getObjectMetadata(FileInfo fileInfo) {
        StringMap params = new StringMap();
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            fileInfo.getMetadata().forEach(params::put);
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata())) {
            fileInfo.getUserMetadata()
                    .forEach((key, value) ->
                            params.put(key.startsWith("x-qn-meta-") ? key : ("x-qn-meta-" + key), value));
        }
        return params;
    }

    /**
     * 获取缩略图对象的元数据
     */
    public StringMap getThObjectMetadata(FileInfo fileInfo) {
        StringMap params = new StringMap();
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            fileInfo.getThMetadata().forEach(params::put);
        }
        if (CollUtil.isNotEmpty(fileInfo.getThUserMetadata())) {
            fileInfo.getThUserMetadata()
                    .forEach((key, value) ->
                            params.put(key.startsWith("x-qn-meta-") ? key : ("x-qn-meta-" + key), value));
        }
        return params;
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try {
            if (!Constant.GeneratePresignedUrl.Method.GET.equalsIgnoreCase(String.valueOf(pre.getMethod()))) {
                throw new FileStorageRuntimeException("七牛云 Kode 仅支持 GET ，如需支持更多功能，可以通过 AWS S3 的 SDK 来使用");
            }
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
            int deadline = (int) (pre.getExpiration().getTime() / 1000);
            String url = getClient().getAuth().privateDownloadUrlWithDeadline(domain + fileKey, deadline);
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url);
            result.setHeaders(new HashMap<>());
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BucketManager manager = getClient().getBucketManager();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                delete(manager, getThFileKey(fileInfo));
            }
            delete(manager, getFileKey(fileInfo));
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
        return true;
    }

    public void delete(BucketManager manager, String filename) throws QiniuException {
        try {
            manager.delete(bucketName, filename);
        } catch (QiniuException e) {
            if (!(e.response != null && e.response.statusCode == 612)) {
                throw e;
            }
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

    public boolean exists(String fileKey) throws QiniuException {
        BucketManager manager = getClient().getBucketManager();
        try {
            com.qiniu.storage.model.FileInfo stat = manager.stat(bucketName, fileKey);
            if (stat != null && (StrUtil.isNotBlank(stat.md5) || StrUtil.isNotBlank(stat.hash))) return true;
        } catch (QiniuException e) {
            if (e.code() == 612) return false;
            throw e;
        }
        return false;
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        String url = getClient().getAuth().privateDownloadUrl(fileInfo.getUrl());
        try (InputStream in = new URL(url).openStream()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        String url = getClient().getAuth().privateDownloadUrl(fileInfo.getThUrl());
        try (InputStream in = new URL(url).openStream()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        BucketManager manager = getClient().getBucketManager();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        com.qiniu.storage.model.FileInfo srcFile;
        try {
            srcFile = manager.stat(bucketName, srcFileKey);
            if (srcFile == null || (StrUtil.isBlank(srcFile.md5) && StrUtil.isBlank(srcFile.hash))) {
                throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
            }
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                manager.copy(bucketName, getThFileKey(srcFileInfo), bucketName, destThFileKey, true);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.fsize);
            manager.copy(bucketName, srcFileKey, bucketName, destFileKey, true);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.fsize);
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    manager.delete(bucketName, destThFileKey);
                } catch (Exception ignored) {
                }
            try {
                manager.delete(bucketName, destFileKey);
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

        BucketManager manager = getClient().getBucketManager();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        com.qiniu.storage.model.FileInfo srcFile;
        try {
            srcFile = manager.stat(bucketName, srcFileKey);
            if (srcFile == null || (StrUtil.isBlank(srcFile.md5) && StrUtil.isBlank(srcFile.hash))) {
                throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, null);
            }
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
                manager.move(bucketName, srcThFileKey, bucketName, destThFileKey, true);
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.fsize);
            manager.move(bucketName, srcFileKey, bucketName, destFileKey, true);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.fsize);
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    manager.move(bucketName, destThFileKey, bucketName, srcThFileKey, true);
                } catch (Exception ignored) {
                }
            try {
                if (exists(srcFileKey)) {
                    manager.delete(bucketName, destFileKey);
                } else {
                    manager.move(bucketName, destFileKey, bucketName, srcFileKey, true);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
