package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import com.obs.services.ObsClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 华为云 OBS 存储
 */
@Slf4j
@Getter
@Setter
public class HuaweiObsFileStorage implements FileStorage {

    /* 存储平台 */
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private String bucketName;
    private String domain;
    private String basePath;

    public ObsClient getObs() {
        return new ObsClient(accessKey,secretKey,endPoint);
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String newFileKey = basePath + pre.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        ObsClient obs = getObs();
        try {
            obs.putObject(bucketName,newFileKey,pre.getFileWrapper().getInputStream());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                fileInfo.setThUrl(fileInfo.getUrl() + pre.getThumbnailSuffix());
                obs.putObject(bucketName,newFileKey + pre.getThumbnailSuffix(),new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException e) {
            obs.deleteObject(bucketName,newFileKey);
            log.error("文件上传失败！platform：{},filename：{}",platform,fileInfo.getOriginalFilename(),e);
        }

        return false;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        ObsClient obs = getObs();
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            obs.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        }
        obs.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        return true;
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return getObs().doesObjectExist(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
    }
}
