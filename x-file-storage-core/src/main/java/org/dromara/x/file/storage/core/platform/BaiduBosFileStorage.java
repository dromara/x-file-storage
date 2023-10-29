package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.baidubce.BceServiceException;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.dromara.x.file.storage.core.FileStorageProperties.BaiduBosConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

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
        } catch (IOException e) {
            if (useMultipartUpload) {
                client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, newFileKey, uploadId));
            } else {
                client.deleteObject(bucketName, newFileKey);
            }
            throw new FileStorageRuntimeException(
                    "文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
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
            throw new FileStorageRuntimeException("不支持的ACL：" + acl);
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
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        int expires = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        return getClient()
                .generatePresignedUrl(bucketName, getFileKey(fileInfo), expires)
                .toString();
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        String key = getThFileKey(fileInfo);
        if (key == null) return null;
        int expires = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        return getClient().generatePresignedUrl(bucketName, key, expires).toString();
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        getClient().setObjectAcl(bucketName, getFileKey(fileInfo), oAcl);
        return true;
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        String key = getThFileKey(fileInfo);
        if (key == null) return false;
        getClient().setObjectAcl(bucketName, key, oAcl);
        return true;
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BosClient client = getClient();
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
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return getClient().doesObjectExist(bucketName, getFileKey(fileInfo));
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        BosObject object = getClient().getObject(bucketName, getFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        BosObject object = getClient().getObject(bucketName, getThFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo,FileInfo destFileInfo,CopyPretreatment pre) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw new FileStorageRuntimeException("文件复制失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath
                    + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
        BosClient client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        ObjectMetadata srcFile;
        try {
            srcFile = client.getObjectMetadata(bucketName, srcFileKey);
        } catch (Exception e) {
            throw new FileStorageRuntimeException(
                    "文件复制失败，无法获取源文件信息！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            CopyObjectRequest request =
                    new CopyObjectRequest(bucketName, getThFileKey(srcFileInfo), bucketName, destThFileKey);
            request.setNewObjectMetadata(getThObjectMetadata(destFileInfo));
            client.copyObject(request);
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
            throw new FileStorageRuntimeException(
                    "文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo, e);
        }
    }
}
