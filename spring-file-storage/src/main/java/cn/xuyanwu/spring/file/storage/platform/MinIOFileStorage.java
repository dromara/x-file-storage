package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import io.minio.*;
import io.minio.errors.*;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

/**
 * MinIO 存储
 */
@Getter
@Setter
public class MinIOFileStorage implements FileStorage {

    /* 存储平台 */
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private String bucketName;
    private String domain;
    private String basePath;
    private MinioClient client;

    /**
     * 单例模式运行，不需要每次使用完再销毁了
     */
    public MinioClient getClient() {
        if (client == null) {
            client = new MinioClient.Builder().credentials(accessKey,secretKey).endpoint(endPoint).build();
        }
        return client;
    }

    /**
     * 仅在移除这个存储平台时调用
     */
    @Override
    public void close() {
        client = null;
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        MinioClient client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(newFileKey)
                    .stream(in,pre.getFileWrapper().getSize(),-1)
                    .contentType(fileInfo.getContentType()).build());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
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
    public boolean delete(FileInfo fileInfo) {
        MinioClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()).build());
            }
            client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()).build());
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
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder().bucket(bucketName).object(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()).build());
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
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()).build())) {
            consumer.accept(in);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        MinioClient client = getClient();
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()).build())) {
            consumer.accept(in);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | ServerException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 XmlParserException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }

    }
}
