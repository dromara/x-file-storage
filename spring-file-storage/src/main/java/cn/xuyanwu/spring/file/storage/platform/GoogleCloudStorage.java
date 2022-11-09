package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Kytrun
 * @version 1.0
 * @date 2022/11/4 9:56
 */
@Getter
@Setter
public class GoogleCloudStorage implements FileStorage {
    private String projectId;
    private String bucketName;
    private String credentialsLocation;
    /* 基础路径 */
    private String basePath;
    /* 存储平台 */
    private String platform;
    /* 访问域名 */
    private String domain;

    private Storage client;

    /**
     * 单例模式运行，不需要每次使用完再销毁了
     */
    public Storage getClient() {
        if (client == null) {
            ServiceAccountCredentials credentialsFromStream;
            try {
                InputStream stream = Files.newInputStream(Paths.get(credentialsLocation));
                credentialsFromStream = ServiceAccountCredentials.fromStream(stream);
            } catch (IOException e) {
                throw new FileStorageRuntimeException("Google Cloud Platform 授权 key 文件获取失败！credentialsLocation：" + credentialsLocation);
            }
            List<String> scopes = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
            ServiceAccountCredentials credentials = credentialsFromStream.toBuilder().setScopes(scopes).build();
            StorageOptions storageOptions = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build();
            client = storageOptions.getService();
        }
        return client;
    }


    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                throw new FileStorageRuntimeException(e);
            }
            client = null;
        }
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);
        Storage client = getClient();

        BlobId blobId = BlobId.of(bucketName, newFileKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(fileInfo.getContentType()).build();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            // 上传原文件
            client.createFrom(blobInfo, in);
            //上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                fileInfo.setThUrl(domain + newThFileKey);
                BlobId thBlobId = BlobId.of(bucketName, newThFileKey);
                BlobInfo thBlobInfo = BlobInfo.newBuilder(thBlobId).setContentType(fileInfo.getThContentType()).build();
                client.createFrom(thBlobInfo, new ByteArrayInputStream(thumbnailBytes));
            }
            return true;
        } catch (IOException e) {
            checkAndDelete(newFileKey);
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }


    /**
     * 检查并删除对象
     * <a href="https://github.com/googleapis/java-storage/blob/main/samples/snippets/src/main/java/com/example/storage/object/DeleteObject.java">Source Example</a>
     *
     * @param fileKey 对象 key
     */
    private void checkAndDelete(String fileKey) {
        Storage client = getClient();
        Blob blob = client.get(bucketName, fileKey);
        if (blob != null) {
            Storage.BlobSourceOption precondition = Storage.BlobSourceOption.generationMatch(blob.getGeneration());
            client.delete(bucketName, fileKey, precondition);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        //删除缩略图
        if (fileInfo.getThFilename() != null) {
            checkAndDelete(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        }
        checkAndDelete(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        return true;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        Storage client = getClient();
        BlobId blobId = BlobId.of(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        return client.get(blobId) != null;
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Storage client = getClient();
        BlobId blobId = BlobId.of(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        ReadChannel readChannel = client.reader(blobId);
        InputStream in = Channels.newInputStream(readChannel);
        consumer.accept(in);
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        Storage client = getClient();
        BlobId thBlobId = BlobId.of(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        ReadChannel readChannel = client.reader(thBlobId);
        InputStream in = Channels.newInputStream(readChannel);
        consumer.accept(in);
    }
}
