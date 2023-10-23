package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.LocalConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 本地文件存储
 */
@Getter
@Setter
@NoArgsConstructor
public class LocalFileStorage implements FileStorage {
    private String basePath;
    private String platform;
    private String domain;


    public LocalFileStorage(LocalConfig config) {
        platform = config.getPlatform();
        basePath = config.getBasePath();
        domain = config.getDomain();
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String path = fileInfo.getPath();

        File newFile = FileUtil.touch(basePath + path,fileInfo.getFilename());
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + path + fileInfo.getFilename());
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，LocalFile 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，LocalFile 不支持设置 Metadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }

        try {
            FileWrapper fileWrapper = pre.getFileWrapper();
            if (fileWrapper.supportTransfer()) {//移动文件，速度较快
                ProgressListener listener = pre.getProgressListener();
                if (listener != null) {
                    listener.start();
                    listener.progress(0,fileWrapper.getSize());
                }
                fileWrapper.transferTo(newFile);
                if (listener != null) {
                    listener.progress(newFile.length(),fileWrapper.getSize());
                    listener.finish();
                }
                if (fileInfo.getSize() == null) fileInfo.setSize(newFile.length());
            } else {//通过输入流写入文件
                InputStreamPlus in = pre.getInputStreamPlus();
                FileUtil.writeFromStream(in,newFile);
                if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            }

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
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
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
