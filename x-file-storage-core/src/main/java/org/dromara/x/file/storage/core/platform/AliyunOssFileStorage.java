package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.model.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.AliyunOssConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;

/**
 * 阿里云 OSS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class AliyunOssFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<OSS> clientFactory;

    public AliyunOssFileStorage(AliyunOssConfig config, FileStorageClientFactory<OSS> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public OSS getClient() {
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
        CannedAccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = getObjectMetadata(fileInfo, fileAcl);
        ProgressListener listener = pre.getProgressListener();
        OSS client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                uploadId = client.initiateMultipartUpload(
                                new InitiateMultipartUploadRequest(bucketName, newFileKey, metadata))
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
                    part.setPartNumber(++i); // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，OSS将返回InvalidArgument错误码。
                    if (listener != null) {
                        part.setProgressListener(e -> {
                            if (e.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                                listener.progress(progressSize.addAndGet(e.getBytes()), fileInfo.getSize());
                            }
                        });
                    }
                    partList.add(client.uploadPart(part).getPartETag());
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, newFileKey, uploadId, partList));
                if (fileAcl != null) client.setObjectAcl(bucketName, newFileKey, fileAcl);
                if (listener != null) listener.finish();
            } else {
                PutObjectRequest request = new PutObjectRequest(bucketName, newFileKey, in, metadata);
                if (listener != null) {
                    AtomicLong progressSize = new AtomicLong();
                    request.setProgressListener(e -> {
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

            // 上传缩略图
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
    public ObjectMetadata getObjectMetadata(FileInfo fileInfo, CannedAccessControlList fileAcl) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (fileInfo.getSize() != null) metadata.setContentLength(fileInfo.getSize());
        metadata.setContentType(fileInfo.getContentType());
        metadata.setObjectAcl(fileAcl);
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
        metadata.setObjectAcl(getAcl(fileInfo.getThFileAcl()));
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
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            return getClient()
                    .generatePresignedUrl(bucketName, getFileKey(fileInfo), expiration)
                    .toString();
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            String key = getThFileKey(fileInfo);
            if (key == null) return null;
            return getClient().generatePresignedUrl(bucketName, key, expiration).toString();
        } catch (Exception e) {
            throw ExceptionFactory.generateThPresignedUrl(fileInfo, platform, e);
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
        OSS client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.deleteObject(bucketName, getThFileKey(fileInfo));
            }
            client.deleteObject(bucketName, getFileKey(fileInfo));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
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
        OSSObject object = getClient().getObject(bucketName, getFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        OSSObject object = getClient().getObject(bucketName, getThFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
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

        OSS client = getClient();

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
                client.copyObject(bucketName, getThFileKey(srcFileInfo), bucketName, destThFileKey);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.getContentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 按照阿里云 OSS 官方文档小于 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制，阿里云 OSS 内部不会自动复制 Metadata 和 ACL，需要重新设置
                CannedAccessControlList fileAcl = getAcl(destFileInfo.getFileAcl());
                ObjectMetadata metadata = getObjectMetadata(destFileInfo, fileAcl);
                uploadId = client.initiateMultipartUpload(
                                new InitiateMultipartUploadRequest(bucketName, destFileKey, metadata))
                        .getUploadId();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<PartETag> partList = new ArrayList<>();
                long progressSize = 0;
                int i = 0;
                while (progressSize < fileSize) {
                    // 设置分片大小为 256 MB。单位为字节。
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    UploadPartCopyRequest part =
                            new UploadPartCopyRequest(bucketName, srcFileKey, bucketName, destFileKey);
                    part.setUploadId(uploadId);
                    part.setPartSize(partSize);
                    part.setBeginIndex(progressSize);
                    part.setPartNumber(++i);
                    partList.add(client.uploadPartCopy(part).getPartETag());
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                CompleteMultipartUploadRequest completeRequest =
                        new CompleteMultipartUploadRequest(bucketName, destFileKey, uploadId, partList);
                completeRequest.setObjectACL(fileAcl);
                client.completeMultipartUpload(completeRequest);
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制，阿里云 OSS 内部会自动复制 Metadata 和 ACL
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                client.copyObject(bucketName, srcFileKey, bucketName, destFileKey);
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
