package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.AzureBlobStorageConfig;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;

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
     * 预签名url默认过期实际
     */
    private Long defaultExpirationTime;

    /**
     * 详见 ModelHelper.populateAndApplyDefaults
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


    public AzureBlobFileStorage(AzureBlobStorageConfig config, FileStorageClientFactory<BlobServiceClient> clientFactory) {
        platform = config.getPlatform();
        containerName = config.getContainerName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        maxConcurrency = config.getMaxConcurrency();
        defaultExpirationTime = config.getDefaultExpirationTime();
        this.clientFactory = clientFactory;
    }

    private BlobContainerClient getBlobContainerClient() {
        return clientFactory.getClient().getBlobContainerClient(containerName);
    }

    /**
     * 获取文件路径对应的client
     *
     * @param fileInfo
     * @return
     */
    public BlobClient getBlobClient(FileInfo fileInfo) {
        fileInfo.setBasePath(basePath);
        BlobContainerClient blobContainerClient = getBlobContainerClient();
        return blobContainerClient.getBlobClient(getFileKey(fileInfo));
    }

    /**
     * 获取文件路径对应的client
     *
     * @param fileInfo
     * @return
     */
    public BlobClient getThBlobClient(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        BlobContainerClient blobContainerClient = getBlobContainerClient();
        return blobContainerClient.getBlobClient(getThFileKey(fileInfo));
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
        BlobClient blobClient = getBlobClient(fileInfo);
        fileInfo.setUrl(blobClient.getBlobUrl());
        Long size = fileInfo.getSize();
        try (InputStream inputStream = pre.getFileWrapper().getInputStream()) {
            ProgressListener listener = pre.getProgressListener();
            //blobClient.upload(listener == null ? inputStream : new ProgressInputStream(inputStream, listener, size), size);
            // 构建上传参数
            BlobParallelUploadOptions blobParallelUploadOptions = new BlobParallelUploadOptions(inputStream);
            ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions();
            parallelTransferOptions.setBlockSizeLong(multipartPartSize);
            parallelTransferOptions.setMaxConcurrency(maxConcurrency);
            parallelTransferOptions.setMaxSingleUploadSizeLong(multipartThreshold);
            if (listener != null) {
                parallelTransferOptions.setProgressListener(progress -> listener.progress(progress, size));
            }
            blobParallelUploadOptions.setParallelTransferOptions(parallelTransferOptions);
            blobParallelUploadOptions.setMetadata(fileInfo.getMetadata());
            blobClient.uploadWithResponse(blobParallelUploadOptions, null, Context.NONE);

            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                BlobClient thBlobClient = getThBlobClient(fileInfo);
                fileInfo.setThUrl(thBlobClient.getBlobUrl());
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(thumbnailBytes);
                thBlobClient.upload(byteArrayInputStream);
            }
            return true;
        } catch (IOException e) {
            blobClient.deleteIfExists();
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BlobClient blobClient = getBlobClient(fileInfo);
        blobClient.deleteIfExists();
        // 删除缩率图
        if (fileInfo.getThFilename() != null) {
            BlobClient thBlobClient = getThBlobClient(fileInfo);
            thBlobClient.deleteIfExists();
        }
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        BlobClient blobClient = getBlobClient(fileInfo);
        return blobClient.exists();
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        BlobClient blobClient = getBlobClient(fileInfo);
        try (InputStream in = blobClient.openInputStream()) {
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
        BlobClient blobClient = getThBlobClient(fileInfo);
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     * 如果存在跨域问题，需要去控制台 （资源共享(CORS)界面）授权允许的跨域规则
     * 生成url的每个参数的含义详见 BlobSasImplUtil.encode
     *
     * @param expiration 到期时间
     */
    @Override
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        BlobClient blobClient = getBlobClient(fileInfo);
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(getBlobServiceSasSignatureValues(expiration));

    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        BlobClient thBlobClient = getThBlobClient(fileInfo);
        return thBlobClient.getBlobUrl() + "?" + thBlobClient.generateSas(getBlobServiceSasSignatureValues(expiration));
    }


    /**
     * 构建sas参数
     * 设置操作权限和，过期时间
     *
     * @param expiration
     * @return
     */
    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(Date expiration) {
        // 设置只读权限
        BlobSasPermission blobPermission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime offsetDateTime;
        // 设置过期时间，默认15分钟
        if (expiration == null) {
            offsetDateTime = now.plusSeconds(defaultExpirationTime);
        } else {
            offsetDateTime = expiration.toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();
        }
        // 生成签名
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(offsetDateTime, blobPermission);
//        blobServiceSasSignatureValues.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
//        blobServiceSasSignatureValues.setProtocol();
//        blobServiceSasSignatureValues.setSasIpRange();
        return blobServiceSasSignatureValues;
    }


    @Override
    public void close() {
        clientFactory.close();
    }

}
