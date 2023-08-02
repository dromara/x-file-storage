package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.TencentCosConfig;
import cn.xuyanwu.spring.file.storage.ProgressListener;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.model.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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


    public TencentCosFileStorage(TencentCosConfig config,FileStorageClientFactory<COSClient> clientFactory) {
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

    public String getFileKey(FileInfo fileInfo) {
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename();
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename();
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        CannedAccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ProgressListener listener = pre.getProgressListener();
        COSClient client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileInfo.getSize());
            metadata.setContentType(fileInfo.getContentType());
            if (useMultipartUpload) {//分片上传
                InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName,newFileKey,metadata);
                initiateMultipartUploadRequest.setCannedACL(fileAcl);
                uploadId = client.initiateMultipartUpload(initiateMultipartUploadRequest).getUploadId();
                List<PartETag> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in,multipartPartSize);
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
                                listener.progress(progressSize.addAndGet(e.getBytes()),fileInfo.getSize());
                            }
                        });
                    }
                    partList.add(client.uploadPart(part).getPartETag());
                }
                client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName,newFileKey,uploadId,partList));
                if (listener != null) listener.finish();
            } else {
                PutObjectRequest request = new PutObjectRequest(bucketName,newFileKey,in,metadata);
                request.setCannedAcl(fileAcl);
                if (listener != null) {
                    AtomicLong progressSize = new AtomicLong();
                    request.setGeneralProgressListener(e -> {
                        if (e.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT) {
                            listener.start();
                        } else if (e.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                            listener.progress(progressSize.addAndGet(e.getBytes()),fileInfo.getSize());
                        } else if (e.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                            listener.finish();
                        }
                    });
                }
                client.putObject(request);
            }

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                ObjectMetadata thMetadata = new ObjectMetadata();
                thMetadata.setContentLength(thumbnailBytes.length);
                thMetadata.setContentType(fileInfo.getThContentType());
                PutObjectRequest request = new PutObjectRequest(bucketName,newThFileKey,new ByteArrayInputStream(thumbnailBytes),thMetadata);
                request.setCannedAcl(getAcl(fileInfo.getThFileAcl()));
                client.putObject(request);
            }

            return true;
        } catch (IOException e) {
            if (useMultipartUpload) {
                client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName,newFileKey,uploadId));
            } else {
                client.deleteObject(bucketName,newFileKey);
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
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
            throw new FileStorageRuntimeException("不支持的ACL：" + acl);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public String generatePresignedUrl(FileInfo fileInfo,Date expiration) {
        return getClient().generatePresignedUrl(bucketName,getFileKey(fileInfo),expiration).toString();
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo,Date expiration) {
        String key = getThFileKey(fileInfo);
        if (key == null) return null;
        return getClient().generatePresignedUrl(bucketName,key,expiration).toString();
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo,Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        getClient().setObjectAcl(bucketName,getFileKey(fileInfo),oAcl);
        return true;
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo,Object acl) {
        CannedAccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        String key = getThFileKey(fileInfo);
        if (key == null) return false;
        getClient().setObjectAcl(bucketName,key,oAcl);
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        COSClient client = getClient();
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            client.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        }
        client.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        return true;
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return getClient().doesObjectExist(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        COSObject object = getClient().getObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        COSObject object = getClient().getObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
