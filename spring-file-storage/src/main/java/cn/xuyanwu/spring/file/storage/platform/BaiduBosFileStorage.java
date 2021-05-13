package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.baidubce.Protocol;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 百度云 BOS 存储
 */
@Getter
@Setter
public class BaiduBosFileStorage implements FileStorage {

    /* 存储平台 */
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private String bucketName;
    private String domain;
    private String basePath;

    public BosClient getOss() {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(accessKey, secretKey));
        config.setEndpoint(endPoint);
        config.setProtocol(Protocol.HTTPS);
        return new BosClient(config);
    }

    /**
     * 关闭
     */
    public void shutdown(BosClient oss) {
        if (oss != null) oss.shutdown();
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        BosClient oss = getOss();
        try {
            oss.putObject(bucketName, newFileKey, pre.getFileWrapper().getInputStream());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                fileInfo.setThUrl(fileInfo.getUrl() + pre.getThumbnailSuffix());
                oss.putObject(bucketName, newFileKey + pre.getThumbnailSuffix(), new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException e) {
            oss.deleteObject(bucketName, newFileKey);
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        } finally {
            shutdown(oss);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        BosClient oss = getOss();
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            oss.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename());
        }
        oss.deleteObject(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        shutdown(oss);
        return true;
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        BosClient oss = getOss();
        boolean b = oss.doesObjectExist(bucketName, fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
        shutdown(oss);
        return b;
    }
}
