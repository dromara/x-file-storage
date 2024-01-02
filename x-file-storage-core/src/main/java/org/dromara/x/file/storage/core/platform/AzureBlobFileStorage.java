package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.AzureBlobStorageConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;

@Getter
@Setter
@NoArgsConstructor
public class AzureBlobFileStorage implements FileStorage {

    /**
     * 平台名称唯一标识，方便多个存储
     */
    private String platform;

    /**
     * 与s3的bucket大差不差
     */
    private String containerName;

    /**
     * 访问url的路径名称
     */
    private String domain;

    /**
     * 基础路径
     */
    private String basePath;

    /**
     * {@link com.azure.storage.blob.implementation.util.ModelHelper#populateAndApplyDefaults(ParallelTransferOptions)}
     * 触发分片上传的阈值
     * 默认值256M
     */
    private Long multipartThreshold;

    /**
     * 触发分片后 ,分片块大小
     * 默认值 4M
     */
    private Long multipartPartSize;

    /**
     * 最大上传并行度
     * 分片后 同时进行上传的 数量
     * 数量太大会占用大量缓冲区
     * 默认 8
     */
    private Integer maxConcurrency;

    private FileStorageClientFactory<BlobServiceClient> clientFactory;

    public AzureBlobFileStorage(
            AzureBlobStorageConfig config, FileStorageClientFactory<BlobServiceClient> clientFactory) {
        platform = config.getPlatform();
        containerName = config.getContainerName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        maxConcurrency = config.getMaxConcurrency();
        this.clientFactory = clientFactory;
    }

    private BlobContainerClient getBlobContainerClient() {
        return clientFactory.getClient().getBlobContainerClient(containerName);
    }

    public BlobClient getBlobClient(FileInfo fileInfo) {
        fileInfo.setBasePath(basePath);
        BlobContainerClient blobContainerClient = getBlobContainerClient();
        return blobContainerClient.getBlobClient(getFileKey(fileInfo));
    }

    public BlobClient getThBlobClient(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        BlobContainerClient blobContainerClient = getBlobContainerClient();
        return blobContainerClient.getBlobClient(getThFileKey(fileInfo));
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(getUrl(getFileKey(fileInfo)));
        BlobClient blobClient = getBlobClient(fileInfo);
        ProgressListener listener = pre.getProgressListener();
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            // 构建上传参数
            BlobParallelUploadOptions blobParallelUploadOptions = new BlobParallelUploadOptions(in);
            ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
            parallelTransferOptions.setBlockSizeLong(multipartPartSize);
            parallelTransferOptions.setMaxConcurrency(maxConcurrency);
            parallelTransferOptions.setMaxSingleUploadSizeLong(multipartThreshold);
            if (listener != null) {
                parallelTransferOptions.setProgressListener(
                        progress -> listener.progress(progress, fileInfo.getSize()));
            }
            blobParallelUploadOptions.setParallelTransferOptions(parallelTransferOptions);
            blobParallelUploadOptions.setMetadata(fileInfo.getMetadata());
            blobClient.uploadWithResponse(blobParallelUploadOptions, null, Context.NONE);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                BlobClient thBlobClient = getThBlobClient(fileInfo);
                fileInfo.setThUrl(getUrl(getThFileKey(fileInfo)));
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(thumbnailBytes);
                thBlobClient.upload(byteArrayInputStream);
            }
            return true;
        } catch (IOException e) {
            try {
                blobClient.deleteIfExists();
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            BlobClient blobClient = getBlobClient(fileInfo);
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                BlobClient thBlobClient = getThBlobClient(fileInfo);
                thBlobClient.deleteIfExists();
            }
            blobClient.deleteIfExists();
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getBlobClient(fileInfo).exists();
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        BlobClient blobClient = getBlobClient(fileInfo);
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);
        BlobClient blobClient = getThBlobClient(fileInfo);
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     * 如果存在跨域问题，需要去控制台 （资源共享(CORS)界面）授权允许的跨域规则
     * 生成url的每个参数的含义{@link com.azure.storage.blob.implementation.util.BlobSasImplUtil#encode(UserDelegationKey, String)}
     * @param expiration 到期时间
     */
    @Override
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            BlobClient blobClient = getBlobClient(fileInfo);
            return blobClient.getBlobUrl() + "?" + blobClient.generateSas(getBlobServiceSasSignatureValues(expiration));
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            String key = getThFileKey(fileInfo);
            if (key == null) return null;
            BlobClient thBlobClient = getThBlobClient(fileInfo);
            return thBlobClient.getBlobUrl() + "?"
                    + thBlobClient.generateSas(getBlobServiceSasSignatureValues(expiration));
        } catch (Exception e) {
            throw ExceptionFactory.generateThPresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);
        // 获取远程文件信息
        BlobClient srcClient = getBlobClient(srcFileInfo);
        BlobClient destClient = getBlobClient(destFileInfo);
        BlobClient srcThClient = getThBlobClient(srcFileInfo);
        BlobClient destThClient = getThBlobClient(destFileInfo);
        if (Boolean.FALSE.equals(srcClient.exists())) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
        }
        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(getUrl(getThFileKey(destFileInfo)));
            try {
                destThClient.beginCopy(srcThClient.getBlobUrl(), Duration.ofSeconds(1));
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        destFileInfo.setUrl(getUrl(getFileKey(destFileInfo)));
        try {
            ProgressListener.quickStart(
                    pre.getProgressListener(), srcClient.getProperties().getBlobSize());
            destClient.beginCopy(srcClient.getBlobUrl(), Duration.ofSeconds(1));
            ProgressListener.quickFinish(
                    pre.getProgressListener(), srcClient.getProperties().getBlobSize());
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    destThClient.deleteIfExists();
                } catch (Exception ignored) {
                }
            try {
                destClient.deleteIfExists();
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    /**
     * 构建sas参数,设置操作权限和过期时间
     *
     * @param expiration
     * @return
     */
    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(Date expiration) {
        // 设置只读权限
        BlobSasPermission blobPermission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime offsetDateTime =
                expiration.toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();

        // 生成签名
        BlobServiceSasSignatureValues blobServiceSasSignatureValues =
                new BlobServiceSasSignatureValues(offsetDateTime, blobPermission);
        return blobServiceSasSignatureValues;
    }

    public String getUrl(String fileKey) {
        return domain + containerName + "/" + fileKey;
    }

    @Override
    public void close() {
        clientFactory.close();
    }
}
