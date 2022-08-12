package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 本地文件存储
 */
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
    public void close() {
    }

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
                fileInfo.setThUrl(domain + path + fileInfo.getThFilename());
                FileUtil.writeBytes(thumbnailBytes,basePath + path + fileInfo.getThFilename());
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(newFile);
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
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

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename())) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        try (InputStream in = FileUtil.getInputStream(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename())) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
