package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.MinioConfig;
import cn.xuyanwu.spring.file.storage.ProgressInputStream;
import cn.xuyanwu.spring.file.storage.ProgressListener;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.function.Consumer;

/**
 * MinIO 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class MinioFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private FileStorageClientFactory<MinioClient> clientFactory;


    public MinioFileStorage(MinioConfig config,FileStorageClientFactory<MinioClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        this.clientFactory = clientFactory;
    }

    public MinioClient getClient() {
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
        if (fileInfo.getFileAcl() != null) {
            throw new FileStorageRuntimeException("文件上传失败，MinIO 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        ProgressListener listener = pre.getProgressListener();
        MinioClient client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            //MinIO 的 SDK 内部会自动分片上传
            Long size = fileInfo.getSize();
            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(newFileKey)
                    .stream(listener == null ? in : new ProgressInputStream(in,listener,size),size,-1)
                    .contentType(fileInfo.getContentType()).build());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.putObject(PutObjectArgs.builder().bucket(bucketName).object(newThFileKey)
                        .stream(new ByteArrayInputStream(thumbnailBytes),thumbnailBytes.length,-1)
                        .contentType(fileInfo.getThContentType()).build());
            }

            return true;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            try {
                client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(newFileKey).build());
            } catch (Exception ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public String generatePresignedUrl(FileInfo fileInfo,Date expiration) {
        int expiry = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(getFileKey(fileInfo))
                .method(Method.GET)
                .expiry(expiry)
                .build();
        try {
            return getClient().getPresignedObjectUrl(args);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            throw new FileStorageRuntimeException("对文件生成可以签名访问的 URL 失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo,Date expiration) {
        String key = getThFileKey(fileInfo);
        if (key == null) return null;
        int expiry = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(key)
                .method(Method.GET)
                .expiry(expiry)
                .build();
        try {
            return getClient().getPresignedObjectUrl(args);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            throw new FileStorageRuntimeException("对文件生成可以签名访问的 URL 失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        MinioClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(getThFileKey(fileInfo)).build());
            }
            client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(getFileKey(fileInfo)).build());
            return true;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
        }
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        MinioClient client = getClient();
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder().bucket(bucketName).object(getFileKey(fileInfo)).build());
            return stat != null && stat.lastModified() != null;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code)) {
                return false;
            }
            throw new FileStorageRuntimeException("查询文件是否存在失败！",e);
        } catch (InsufficientDataException | InternalException | ServerException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！",e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        MinioClient client = getClient();
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(bucketName).object(getFileKey(fileInfo)).build())) {
            consumer.accept(in);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        MinioClient client = getClient();
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(bucketName).object(getThFileKey(fileInfo)).build())) {
            consumer.accept(in);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }

    }
}
