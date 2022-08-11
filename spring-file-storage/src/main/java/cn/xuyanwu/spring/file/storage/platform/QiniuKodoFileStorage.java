package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

/**
 * 七牛云 Kodo 存储
 */
@Getter
@Setter
public class QiniuKodoFileStorage implements FileStorage {

    /* 存储平台 */
    private String platform;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String domain;
    private String basePath;
    private Region region;

    public String getToken() {
        return getAuth().uploadToken(bucketName);
    }

    public Auth getAuth() {
        return Auth.create(accessKey,secretKey);
    }

    public BucketManager getBucketManager() {
        return new BucketManager(getAuth(),new Configuration(Region.autoRegion()));
    }

    public UploadManager getUploadManager() {
        return new UploadManager(new Configuration(Region.autoRegion()));
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        try {
            UploadManager uploadManager = getUploadManager();
            String token = getToken();
            uploadManager.put(pre.getFileWrapper().getInputStream(),newFileKey,token,null,fileInfo.getContentType());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                fileInfo.setThUrl(domain + newThFileKey);
                uploadManager.put(new ByteArrayInputStream(thumbnailBytes),newThFileKey,token,null,fileInfo.getThContentType());
            }

            return true;
        } catch (IOException e) {
            try {
                getBucketManager().delete(bucketName,newFileKey);
            } catch (QiniuException ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BucketManager manager = getBucketManager();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                manager.delete(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
            }
            manager.delete(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        } catch (QiniuException e) {
            throw new FileStorageRuntimeException("删除文件失败！" + e.code() + "，" + e.response.toString(),e);
        }
        return true;
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        BucketManager manager = getBucketManager();
        try {
            com.qiniu.storage.model.FileInfo stat = manager.stat(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
            if (stat != null && stat.md5 != null) return true;
        } catch (QiniuException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！" + e.code() + "，" + e.response.toString(),e);
        }
        return false;
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        String url = getAuth().privateDownloadUrl(fileInfo.getUrl());
        try (InputStream in = new URL(url).openStream()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThUrl())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        String url = getAuth().privateDownloadUrl(fileInfo.getThUrl());
        try (InputStream in = new URL(url).openStream()) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
