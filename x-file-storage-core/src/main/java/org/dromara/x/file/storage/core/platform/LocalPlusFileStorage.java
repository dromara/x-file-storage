package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.LocalPlusConfig;
import org.dromara.x.file.storage.core.ProgressInputStream;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 本地文件存储升级版
 */
@Getter
@Setter
@NoArgsConstructor
public class LocalPlusFileStorage implements FileStorage {
    private String basePath;
    private String storagePath;
    private String platform;
    private String domain;


    public LocalPlusFileStorage(LocalPlusConfig config) {
        platform = config.getPlatform();
        basePath = config.getBasePath();
        domain = config.getDomain();
        storagePath = config.getStoragePath();
    }

    /**
     * 获取本地绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
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
            throw new FileStorageRuntimeException("文件上传失败，LocalFile 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，LocalFile 不支持设置 Metadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        ProgressListener listener = pre.getProgressListener();

        try {
            File newFile = FileUtil.touch(getAbsolutePath(newFileKey));
            FileWrapper fileWrapper = pre.getFileWrapper();

            if (listener == null) {
                if (fileWrapper.supportTransfer()) {
                    fileWrapper.transferTo(newFile);
                } else {
                    FileUtil.writeFromStream(fileWrapper.getInputStream(),newFile);
                }
            } else {
                if (fileWrapper.supportTransfer()) {
                    listener.start();
                    listener.progress(0,fileWrapper.getSize());
                    fileWrapper.transferTo(newFile);
                    listener.progress(fileWrapper.getSize(),fileWrapper.getSize());
                    listener.finish();
                } else {
                    FileUtil.writeFromStream(new ProgressInputStream(fileWrapper.getInputStream(),listener,fileWrapper.getSize()),newFile);
                }
            }

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                FileUtil.writeBytes(thumbnailBytes,getAbsolutePath(newThFileKey));
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(getAbsolutePath(newFileKey));
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
    }


    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            FileUtil.del(getAbsolutePath(getThFileKey(fileInfo)));
        }
        return FileUtil.del(getAbsolutePath(getFileKey(fileInfo)));
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return new File(getAbsolutePath(getFileKey(fileInfo))).exists();
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
