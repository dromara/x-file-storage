package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
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
import org.dromara.x.file.storage.core.FileStorageProperties.TencentCosConfig;
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
 * 腾讯云 COS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class TencentCosFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<COSClient> clientFactory;

    public TencentCosFileStorage(TencentCosConfig config, FileStorageClientFactory<COSClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public COSClient getClient() {
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
        CannedAccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        ProgressListener listener = pre.getProgressListener();
        COSClient client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                        new InitiateMultipartUploadRequest(bucketName, newFileKey, metadata);
                initiateMultipartUploadRequest.setCannedACL(fileAcl);
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
                    part.setPartSize(bytes.length); // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为默认为5MB，可在控制台自行调整。
                    part.setPartNumber(++i); // 设置要上传的分块编号，从 1 开始
                    if (listener != null) {
                        part.setGeneralProgressListener(e -> {
                            if (e.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                                listener.progress(progressSize.addAndGet(e.getBytes()), fileInfo.getSize());
                            }
                        });
                    }
                    partList.add(client.uploadPart(part).getPartETag());
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, newFileKey, uploadId, partList));
                if (listener != null) listener.finish();
            } else {
                PutObjectRequest request = new PutObjectRequest(bucketName, newFileKey, in, metadata);
                request.setCannedAcl(fileAcl);
                if (listener != null) {
                    AtomicLong progressSize = new AtomicLong();
                    request.setGeneralProgressListener(e -> {
                        if (e.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT) {
                            listener.start();
                        } else if (e.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                            listener.progress(progressSize.addAndGet(e.getBytes()), fileInfo.getSize());
                        } else if (e.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                            listener.finish();
                        }
                    });
                }
                client.putObject(request);
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                ObjectMetadata thMetadata = getThObjectMetadata(fileInfo);
                PutObjectRequest request = new PutObjectRequest(
                        bucketName, newThFileKey, new ByteArrayInputStream(thumbnailBytes), thMetadata);
                request.setCannedAcl(getAcl(fileInfo.getThFileAcl()));
                client.putObject(request);
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
        CannedAccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        COSClient client = getClient();
        try {
            InitiateMultipartUploadRequest request =
                    new InitiateMultipartUploadRequest(bucketName, newFileKey, metadata);
            request.setCannedACL(fileAcl);
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
        COSClient client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // 腾讯云 COS 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);
            UploadPartRequest part = new UploadPartRequest();
            part.setBucketName(bucketName);
            part.setKey(newFileKey);
            part.setUploadId(fileInfo.getUploadId());
            part.setInputStream(in);
            part.setPartSize(partSize);
            part.setPartNumber(pre.getPartNumber());
            UploadPartResult result = client.uploadPart(part);
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(result.getETag());
            filePartInfo.setPartNumber(result.getPartNumber());
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
        COSClient client = getClient();
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
        COSClient client = getClient();
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
        COSClient client = getClient();
        try {
            ListPartsRequest request = new ListPartsRequest(bucketName, newFileKey, fileInfo.getUploadId());
            request.setMaxParts(pre.getMaxParts());
            request.setPartNumberMarker(pre.getPartNumberMarker());
            PartListing result = client.listParts(request);
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
        COSClient client = getClient();
        try {
            ListObjectsRequest request = new ListObjectsRequest();
            request.setBucketName(bucketName);
            request.setMaxKeys(pre.getMaxFiles());
            request.setMarker(pre.getMarker());
            request.setDelimiter("/");
            request.setPrefix(basePath + pre.getPath() + pre.getFilenamePrefix());
            ObjectListing result = client.listObjects(request);
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

            list.setFileList(result.getObjectSummaries().stream()
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
        COSClient client = getClient();
        try {
            COSObject file = null;
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
            info.setContentMd5(metadata.getContentMD5());
            info.setLastModified(metadata.getLastModified());
            if (metadata.getRawMetadata() != null) info.setMetadata(new HashMap<>(metadata.getRawMetadata()));
            if (metadata.getUserMetadata() != null) info.setUserMetadata(new HashMap<>(metadata.getUserMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取文件的访问控制列表
     */
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
            return null;
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 获取对象的元数据
     */
    public ObjectMetadata getObjectMetadata(FileInfo fileInfo) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (fileInfo.getSize() != null) metadata.setContentLength(fileInfo.getSize());
        if (fileInfo.getContentType() != null) metadata.setContentType(fileInfo.getContentType());
        metadata.setUserMetadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            fileInfo.getMetadata().forEach(metadata::setHeader);
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
        metadata.setUserMetadata(fileInfo.getThUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            fileInfo.getThMetadata().forEach(metadata::setHeader);
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
            request.setExpiration(pre.getExpiration());
            request.setMethod(Tools.getEnum(HttpMethodName.class, pre.getMethod()));
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-cos-meta-") ? e.getKey() : "x-cos-meta-" + e.getKey(),
                            Map.Entry::getValue)));
            headers.forEach(request::putCustomRequestHeader);
            pre.getQueryParams().forEach(request::addRequestParameter);
            pre.getResponseHeaders().forEach((k, v) -> request.addRequestParameter("response-" + k.toLowerCase(), v));
            URL url = getClient().generatePresignedUrl(request);
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url.toString());
            result.setHeaders(request.getCustomRequestHeaders());
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
        COSClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
            }
            client.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient()
                    .doesObjectExist(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (COSObject object = getClient()
                        .getObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
                InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (COSObject object = getClient()
                        .getObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
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

        COSClient client = getClient();

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
                request.setCannedAccessControlList(getAcl(destFileInfo.getFileAcl()));
                client.copyObject(request);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.getContentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 小于 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制，腾讯云 COS 内部不会自动复制 Metadata 和 ACL，需要重新设置
                CannedAccessControlList fileAcl = getAcl(destFileInfo.getFileAcl());
                ObjectMetadata metadata = getObjectMetadata(destFileInfo);
                InitiateMultipartUploadRequest initRequest =
                        new InitiateMultipartUploadRequest(bucketName, destFileKey, metadata);
                initRequest.setCannedACL(fileAcl);
                uploadId = client.initiateMultipartUpload(initRequest).getUploadId();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<PartETag> partList = new ArrayList<>();
                long progressSize = 0;
                int i = 0;
                while (progressSize < fileSize) {
                    // 设置分片大小为 256 MB。单位为字节。
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    CopyPartRequest part = new CopyPartRequest();
                    part.setSourceBucketName(bucketName);
                    part.setSourceKey(srcFileKey);
                    part.setDestinationBucketName(bucketName);
                    part.setDestinationKey(destFileKey);
                    part.setUploadId(uploadId);
                    part.setFirstByte(progressSize);
                    part.setLastByte(progressSize + partSize - 1);
                    part.setPartNumber(++i);
                    partList.add(client.copyPart(part).getPartETag());
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, destFileKey, uploadId, partList));
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制，腾讯云 COS 内部会自动复制 Metadata ，但是 ACL 需要重新设置
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                CopyObjectRequest request = new CopyObjectRequest(bucketName, srcFileKey, bucketName, destFileKey);
                request.setCannedAccessControlList(getAcl(destFileInfo.getFileAcl()));
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
