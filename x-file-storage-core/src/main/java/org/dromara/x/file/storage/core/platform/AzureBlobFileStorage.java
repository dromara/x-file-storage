package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.io.ByteArrayInputStream;
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

    public BlobClient getBlobClient(String fileKey) {
        if (StrUtil.isBlank(fileKey)) return null;
        return clientFactory.getClient().getBlobContainerClient(containerName).getBlobClient(fileKey);
    }

    public String getUrl(String fileKey) {
        return domain + containerName + "/" + fileKey;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        Check.uploadNotSupportAcl(getPlatform(), fileInfo, pre);
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(getUrl(newFileKey));
        ProgressListener listener = pre.getProgressListener();
        BlobClient blobClient = getBlobClient(newFileKey);
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            // 构建上传参数，经测试，大文件会自动多线程分片上传，且无需指定文件大小
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(in);
            setMetadata(options, fileInfo);
            options.setParallelTransferOptions(new ParallelTransferOptions()
                    .setBlockSizeLong(multipartPartSize)
                    .setMaxConcurrency(maxConcurrency)
                    .setMaxSingleUploadSizeLong(multipartThreshold));
            if (listener != null) {
                options.getParallelTransferOptions()
                        .setProgressListener(progressSize -> listener.progress(progressSize, fileInfo.getSize()));
            }
            ProgressListener.quickStart(listener, fileInfo.getSize());
            blobClient.uploadWithResponse(options, null, Context.NONE);
            ProgressListener.quickFinish(listener);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(getUrl(newThFileKey));
                BlobParallelUploadOptions thOptions =
                        new BlobParallelUploadOptions(new ByteArrayInputStream(thumbnailBytes));
                setThMetadata(thOptions, fileInfo);
                getBlobClient(newThFileKey).uploadWithResponse(thOptions, null, Context.NONE);
            }
            return true;
        } catch (Exception e) {
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

    /**
     * 设置对象的元数据
     */
    public void setMetadata(BlobParallelUploadOptions options, FileInfo fileInfo) {
        options.setMetadata(fileInfo.getUserMetadata());
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (StrUtil.isNotBlank(fileInfo.getContentType())) headers.setContentType(fileInfo.getContentType());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), headers, copyOptions);
        }
        options.setHeaders(headers);
    }

    /**
     * 设置缩略图对象的元数据
     */
    public void setThMetadata(BlobParallelUploadOptions options, FileInfo fileInfo) {
        options.setMetadata(fileInfo.getThUserMetadata());
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (StrUtil.isNotBlank(fileInfo.getThContentType())) headers.setContentType(fileInfo.getThContentType());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getThMetadata(), headers, copyOptions);
        }
        options.setHeaders(headers);
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                getBlobClient(getThFileKey(fileInfo)).deleteIfExists();
            }
            getBlobClient(getFileKey(fileInfo)).deleteIfExists();
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getBlobClient(getFileKey(fileInfo)).exists();
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        BlobClient blobClient = getBlobClient(getFileKey(fileInfo));
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);
        BlobClient blobClient = getBlobClient(getThFileKey(fileInfo));
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (Exception e) {
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
            BlobClient blobClient = getBlobClient(getFileKey(fileInfo));
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
            BlobClient thBlobClient = getBlobClient(getThFileKey(fileInfo));
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

    /**
     * 等待复制完成并处理复制结果
     */
    public void awaitCopy(SyncPoller<BlobCopyInfo, Void> copySyncPoller) {
        while (true) {
            PollResponse<BlobCopyInfo> copyInfo = copySyncPoller.poll();
            CopyStatusType copyStatus = copyInfo.getValue().getCopyStatus();
            if (copyStatus == CopyStatusType.PENDING) continue;
            else if (copyStatus == CopyStatusType.SUCCESS) break;
            else throw new RuntimeException(copyStatus.toString());
        }
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);
        // 获取远程文件信息
        String destFileKey = getFileKey(destFileInfo);
        String destThFileKey = getThFileKey(destFileInfo);
        BlobClient srcClient = getBlobClient(getFileKey(srcFileInfo));
        BlobClient destClient = getBlobClient(destFileKey);
        BlobClient srcThClient = getBlobClient(getThFileKey(srcFileInfo));
        BlobClient destThClient = getBlobClient(destThFileKey);
        if (!Boolean.TRUE.equals(srcClient.exists())) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
        }
        // 复制缩略图文件
        if (destThClient != null) {
            destFileInfo.setThUrl(getUrl(destThFileKey));
            try {
                awaitCopy(destThClient.beginCopy(srcThClient.getBlobUrl(), Duration.ofSeconds(1)));

            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        destFileInfo.setUrl(getUrl(destFileKey));
        try {
            long size = srcClient.getProperties().getBlobSize();
            ProgressListener.quickStart(pre.getProgressListener(), size);
            awaitCopy(destClient.beginCopy(srcClient.getBlobUrl(), Duration.ofSeconds(1)));
            ProgressListener.quickFinish(pre.getProgressListener(), size);
        } catch (Exception e) {
            if (destThClient != null)
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
     */
    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(Date expiration) {
        // 设置只读权限
        BlobSasPermission blobPermission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime offsetDateTime =
                expiration.toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();

        // 生成签名
        return new BlobServiceSasSignatureValues(offsetDateTime, blobPermission);
    }

    @Override
    public void close() {
        clientFactory.close();
    }
}
