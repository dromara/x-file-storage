package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.FileUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * 本地文件存储
 */
@Slf4j
@Getter
@Setter
public class LocalFileStorage implements FileStorage {

    /* 本地存储路径*/
    private String basePath;
    /* 存储平台 */
    private String platform;
    /* 访问域名 */
    private String domain;

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String path = fileInfo.getPath();

        File newFile = FileUtil.touch(basePath + path,fileInfo.getFilename());
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + path + fileInfo.getFilename());

        try {
            pre.getFileWrapper().transferTo(newFile);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                fileInfo.setThUrl(fileInfo.getUrl() + pre.getThumbnailSuffix());
                FileUtil.writeBytes(thumbnailBytes,newFile.getPath() + pre.getThumbnailSuffix());
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(newFile);
            log.error("文件上传失败！platform：{},filename：{}",platform,fileInfo.getOriginalFilename(),e);
        }

        return false;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            FileUtil.del(new File(fileInfo.getBasePath() + fileInfo.getPath(),fileInfo.getThFilename()));
        }
        return FileUtil.del(new File(fileInfo.getBasePath() + fileInfo.getPath(),fileInfo.getFilename()));
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return new File(fileInfo.getBasePath() + fileInfo.getPath(),fileInfo.getFilename()).exists();
    }
}
