package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.BceServiceException;
import com.baidubce.http.HttpMethodName;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.BaiduBosConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 百度云 BOS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class BaiduBosFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<BosClient> clientFactory;

    public BaiduBosFileStorage(BaiduBosConfig config, FileStorageClientFactory<BosClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public BosClient getClient() {
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
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        ProgressListener listener = pre.getProgressListener();
        BosClient client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                        new InitiateMultipartUploadRequest(bucketName, newFileKey);
                initiateMultipartUploadRequest.setObjectMetadata(metadata);
                uploadId = client.initiateMultipartUpload(initiateMultipartUploadRequest)
                        .getUploadId();
                List<PartETag> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, multipartPartSize);
                    if (bytes == null || bytes.length == 0) break;
                    UploadPartRequest part = new UploadPartRequest();
                    part.setBucketName(bucketName);
                    part.setKey(newFileKey);
                    part.setUploadId(uploadId);
                    part.setInputStream(new ByteArrayInputStream(bytes));
                    part.setPartSize(bytes.length); // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为100 KB。
                    part.setPartNumber(
                            ++i); // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，BosClient将返回InvalidArgument错误码。
                    if (listener != null) {
                        part.setProgressCallback(new BosProgressCallback<Object>() {
                            @Override
                            public void onProgress(long currentSize, long totalSize, Object data) {
                                listener.progress(progressSize.get() + currentSize, fileInfo.getSize());
                            }
                        });
                    }
                    partList.add(client.uploadPart(part).getPartETag());
                    progressSize.addAndGet(bytes.length);
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, newFileKey, uploadId, partList));
                if (listener != null) listener.finish();
            } else {
                PutObjectRequest request = new PutObjectRequest(bucketName, newFileKey, in, metadata);
                if (listener != null) {
                    listener.start();
                    request.setProgressCallback(new BosProgressCallback<Object>() {
                        @Override
                        public void onProgress(long currentSize, long totalSize, Object data) {
                            listener.progress(currentSize, fileInfo.getSize());
                        }
                    });
                }
                client.putObject(request);
                if (listener != null) listener.finish();
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);

                client.putObject(
                        bucketName,
                        newThFileKey,
                        new ByteArrayInputStream(thumbnailBytes),
                        getThObjectMetadata(fileInfo));
            }

            return true;
        } catch (Exception e) {
            try {
                if (useMultipartUpload) {
                    client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, newFileKey, uploadId));
                } else {
                    client.deleteObject(bucketName, newFileKey);
                }
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
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        BosClient client = getClient();
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, newFileKey);
            request.setObjectMetadata(metadata);
            String uploadId = client.initiateMultipartUpload(request).getUploadId();
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        BosClient client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // 百度云 BOS 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);
            UploadPartRequest part = new UploadPartRequest();
            part.setBucketName(bucketName);
            part.setKey(newFileKey);
            part.setUploadId(fileInfo.getUploadId());
            part.setInputStream(in);
            part.setPartSize(partSize);
            part.setPartNumber(pre.getPartNumber());
            PartETag partETag = client.uploadPart(part).getPartETag();
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(partETag.getETag());
            filePartInfo.setPartNumber(partETag.getPartNumber());
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
        BosClient client = getClient();
        try {
            List<PartETag> partList = pre.getPartInfoList().stream()
                    .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                    .collect(Collectors.toList());
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            client.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(bucketName, newFileKey, fileInfo.getUploadId(), partList));
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        BosClient client = getClient();
        try {
            client.abortMultipartUpload(
                    new AbortMultipartUploadRequest(bucketName, newFileKey, fileInfo.getUploadId()));
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        BosClient client = getClient();
        try {
            ListPartsRequest request = new ListPartsRequest(bucketName, newFileKey, fileInfo.getUploadId());
            request.setMaxParts(pre.getMaxParts());
            request.setPartNumberMarker(pre.getPartNumberMarker());
            ListPartsResponse result = client.listParts(request);
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(result.getParts().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.getETag());
                        filePartInfo.setPartNumber(p.getPartNumber());
                        filePartInfo.setPartSize(p.getSize());
                        filePartInfo.setLastModified(p.getLastModified());
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));
            list.setMaxParts(result.getMaxParts());
            list.setIsTruncated(result.isTruncated());
            list.setPartNumberMarker(result.getPartNumberMarker());
            list.setNextPartNumberMarker(result.getNextPartNumberMarker());
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
        BosClient client = getClient();
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucketName);
            request.setMaxKeys(pre.getMaxFiles());
            request.setMarker(pre.getMarker());
            request.setDelimiter("/");
            request.setPrefix(basePath + pre.getPath() + pre.getFilenamePrefix());
            ListObjectsResponse result = client.listObjects(request);
            ListFilesResult list = new ListFilesResult();

            list.setDirList(result.getCommonPrefixes().stream()
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

            list.setFileList(result.getContents().stream()
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.getKey()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.getSize());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(item.getETag());
                        info.setLastModified(item.getLastModified());
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.getMaxKeys());
            list.setIsTruncated(result.isTruncated());
            list.setMarker(result.getMarker());
            list.setNextMarker(result.getNextMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        BosClient client = getClient();
        try {
            BosObject file = null;
            ObjectMetadata metadata;
            try {
                file = client.getObject(bucketName, fileKey);
                metadata = file.getObjectMetadata();
            } catch (Exception e) {
                return null;
            } finally {
                IoUtil.close(file);
            }
            if (metadata == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.getKey()));
            info.setUrl(domain + fileKey);
            info.setSize(metadata.getContentLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(metadata.getETag());
            info.setContentDisposition(metadata.getContentDisposition());
            info.setContentType(metadata.getContentType());
            info.setContentMd5(metadata.getContentMd5());
            info.setLastModified(metadata.getLastModified());
            info.setMetadata(BeanUtil.beanToMap(metadata, false, true));
            info.getMetadata().remove("userMetadata");
            if (metadata.getUserMetadata() != null) info.setUserMetadata(new HashMap<>(metadata.getUserMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    public CannedAccessControlList getAcl(Object acl) {
        if (acl instanceof CannedAccessControlList) {
            return (CannedAccessControlList) acl;
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl;
            for (CannedAccessControlList item : CannedAccessControlList.values()) {
                if (item.toString().equals(sAcl)) {
                    return item;
                }
            }
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
        return null;
    }

    /**
     * 获取对象的元数据
     */
    public ObjectMetadata getObjectMetadata(FileInfo fileInfo) {
        CannedAccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = new ObjectMetadata();
        if (fileInfo.getSize() != null) metadata.setContentLength(fileInfo.getSize());
        metadata.setContentType(fileInfo.getContentType());
        if (fileAcl != null) metadata.setxBceAcl(fileAcl.toString());
        metadata.setUserMetadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), metadata, copyOptions);
        }
        return metadata;
    }

    /**
     * 获取缩略图对象的元数据
     */
    public ObjectMetadata getThObjectMetadata(FileInfo fileInfo) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileInfo.getThSize());
        metadata.setContentType(fileInfo.getThContentType());
        CannedAccessControlList thFileAcl = getAcl(fileInfo.getThFileAcl());
        if (thFileAcl != null) metadata.setxBceAcl(thFileAcl.toString());
        metadata.setUserMetadata(fileInfo.getThUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getThMetadata(), metadata, copyOptions);
        }
        return metadata;
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try {
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileKey);
            request.setExpiration((int) ((pre.getExpiration().getTime() - System.currentTimeMillis()) / 1000));
            request.setMethod(Tools.getEnum(HttpMethodName.class, pre.getMethod()));
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-bce-meta-") ? e.getKey() : "x-bce-meta-" + e.getKey(),
                            Map.Entry::getValue)));
            headers.forEach(request::addRequestHeaders);
            pre.getQueryParams().forEach(request::addRequestParameter);
            pre.getResponseHeaders()
                    .forEach((k, v) ->
                            request.addRequestParameter(NamingCase.toCamelCase("response-" + k.toLowerCase(), '-'), v));
            URL url = getClient().generatePresignedUrl(request);
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url.toString());
            result.setHeaders(headers);
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            getClient().setObjectAcl(bucketName, getFileKey(fileInfo), oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        String key = getThFileKey(fileInfo);
        if (key == null) return false;
        try {
            getClient().setObjectAcl(bucketName, key, oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setThFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BosClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                try {
                    client.deleteObject(bucketName, getThFileKey(fileInfo));
                } catch (BceServiceException e) {
                    if (!"NoSuchKey".equals(e.getErrorCode())) throw e;
                }
            }
            try {
                client.deleteObject(bucketName, getFileKey(fileInfo));
            } catch (BceServiceException e) {
                if (!"NoSuchKey".equals(e.getErrorCode())) throw e;
            }
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient().doesObjectExist(bucketName, getFileKey(fileInfo));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (BosObject object = getClient().getObject(bucketName, getFileKey(fileInfo));
                InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (BosObject object = getClient().getObject(bucketName, getThFileKey(fileInfo));
                InputStream in = object.getObjectContent()) {
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
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        BosClient client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        ObjectMetadata srcFile;
        try {
            srcFile = client.getObjectMetadata(bucketName, srcFileKey);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                CopyObjectRequest request =
                        new CopyObjectRequest(bucketName, getThFileKey(srcFileInfo), bucketName, destThFileKey);
                request.setNewObjectMetadata(getThObjectMetadata(destFileInfo));
                client.copyObject(request);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.getContentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 按照百度云 BOS 官方文档小于 5GB，但为了统一，这里还是 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制，百度云 BOS 内部不会自动复制 Metadata 和 ACL，需要重新设置
                ObjectMetadata metadata = getObjectMetadata(destFileInfo);
                uploadId = client.initiateMultipartUpload(
                                new InitiateMultipartUploadRequest(bucketName, destFileKey).withMetadata(metadata))
                        .getUploadId();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<PartETag> partList = new ArrayList<>();
                long progressSize = 0;
                for (int i = 1; progressSize < fileSize; i++) {
                    // 设置分片大小为 256 MB。单位为字节。
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    UploadPartCopyRequest part = new UploadPartCopyRequest();
                    part.setBucketName(bucketName);
                    part.setKey(destFileKey);
                    part.setSourceBucketName(bucketName);
                    part.setSourceKey(srcFileKey);
                    part.setUploadId(uploadId);
                    part.setPartSize(partSize);
                    part.setOffSet(progressSize);
                    part.setPartNumber(i);
                    UploadPartCopyResponse partCopyResponse = client.uploadPartCopy(part);
                    partList.add(new PartETag(part.getPartNumber(), partCopyResponse.getETag()));
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, destFileKey, uploadId, partList, metadata));
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制，华为云 OBS 内部会自动复制 Metadata ，但是 ACL 需要重新设置，因为 ACL 包含在 Metadata 中，所以这里全部重新设置
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                CopyObjectRequest request = new CopyObjectRequest(bucketName, srcFileKey, bucketName, destFileKey);
                request.withNewObjectMetadata(getObjectMetadata(destFileInfo));
                client.copyObject(request);
                ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
            }
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    client.deleteObject(bucketName, destThFileKey);
                } catch (Exception ignored) {
                }
            try {
                if (useMultipartCopy) {
                    client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, destFileKey, uploadId));
                } else {
                    client.deleteObject(bucketName, destFileKey);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
