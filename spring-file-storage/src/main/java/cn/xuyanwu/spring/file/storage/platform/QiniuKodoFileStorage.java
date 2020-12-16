package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 七牛云 Kodo 存储
 */
@Slf4j
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
            uploadManager.put(pre.getFileWrapper().getInputStream(),newFileKey,token,null,null);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                fileInfo.setThUrl(fileInfo.getUrl() + pre.getThumbnailSuffix());
                uploadManager.put(new ByteArrayInputStream(thumbnailBytes),newFileKey + pre.getThumbnailSuffix(),token,null,null);
            }

            return true;
        } catch (IOException e) {
            try {
                getBucketManager().delete(bucketName,newFileKey);
            } catch (QiniuException ignored) {
            }
            log.error("文件上传失败！platform：{},filename：{}",platform,fileInfo.getOriginalFilename(),e);
        }

        return false;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BucketManager manager = getBucketManager();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                manager.delete(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
            }
            manager.delete(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        } catch (QiniuException ex) {
            log.error("删除文件失败：{}，{}",ex.code(),ex.response.toString());
            return false;
        }
        return true;
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        BucketManager manager = getBucketManager();
        try {
            com.qiniu.storage.model.FileInfo stat = manager.stat(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
            if (stat != null && stat.md5 != null) return true;
        } catch (QiniuException ex) {
            log.error("查询文件是否存在失败：{}，{}",ex.code(),ex.response.toString());
        }
        return false;
    }
}
