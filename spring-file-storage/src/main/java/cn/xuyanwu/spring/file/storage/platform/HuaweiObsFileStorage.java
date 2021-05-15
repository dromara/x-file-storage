package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.obs.services.ObsClient;
import com.obs.services.model.ObsObject;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 华为云 OBS 存储
 */
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

    public void close(ObsClient obs) {
        IoUtil.close(obs);
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
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                fileInfo.setThUrl(domain + newThFileKey);
                obs.putObject(bucketName,newThFileKey,new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException e) {
            obs.deleteObject(bucketName,newFileKey);
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        } finally {
            close(obs);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        ObsClient obs = getObs();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                obs.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
            }
            obs.deleteObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
            return true;
        } finally {
            close(obs);
        }
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        ObsClient obs = getObs();
        try {
            return obs.doesObjectExist(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        } finally {
            close(obs);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        ObsClient obs = getObs();
        try {
            ObsObject object = obs.getObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
            try (InputStream in = object.getObjectContent()) {
                consumer.accept(in);
            } catch (IOException e) {
                throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
            }
        } finally {
            close(obs);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        ObsClient obs = getObs();
        try {
            ObsObject object = obs.getObject(bucketName,fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
            try (InputStream in = object.getObjectContent()) {
                consumer.accept(in);
            } catch (IOException e) {
                throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
            }
        } finally {
            close(obs);
        }
    }
}
