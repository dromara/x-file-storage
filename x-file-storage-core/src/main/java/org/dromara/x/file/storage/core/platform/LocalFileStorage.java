package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
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

    /**
     * 获取本地绝对路径
     */
    public String getAbsolutePath(String path) {
        return basePath + path;
    }

    public String getFileKey(FileInfo fileInfo) {
        return fileInfo.getPath() + fileInfo.getFilename();
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getPath() + fileInfo.getThFilename();
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件上传失败，LocalFile 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，LocalFile 不支持设置 Metadata！platform：" + platform + "，filename："
                    + fileInfo.getOriginalFilename());
        }

        try {
            File newFile = FileUtil.touch(getAbsolutePath(newFileKey));
            FileWrapper fileWrapper = pre.getFileWrapper();
            if (fileWrapper.supportTransfer()) { // 移动文件，速度较快
                ProgressListener listener = pre.getProgressListener();
                if (listener != null) {
                    listener.start();
                    listener.progress(0, fileWrapper.getSize());
                }
                fileWrapper.transferTo(newFile);
                if (listener != null) {
                    listener.progress(newFile.length(), fileWrapper.getSize());
                    listener.finish();
                }
                if (fileInfo.getSize() == null) fileInfo.setSize(newFile.length());
            } else { // 通过输入流写入文件
                InputStreamPlus in = pre.getInputStreamPlus();
                FileUtil.writeFromStream(in, newFile);
                if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            }

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                FileUtil.writeBytes(thumbnailBytes, getAbsolutePath(newThFileKey));
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(getAbsolutePath(newFileKey));
            throw new FileStorageRuntimeException(
                    "文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) { // 删除缩略图
            FileUtil.del(getAbsolutePath(getThFileKey(fileInfo)));
        }
        return FileUtil.del(getAbsolutePath(getFileKey(fileInfo)));
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return new File(getAbsolutePath(getFileKey(fileInfo))).exists();
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }

    @Override
    public boolean isSupportCopy() {
        return true;
    }

    @Override
    public void copy(FileInfo srcFileInfo, FileInfo destFileInfo, ProgressListener progressListener) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw new FileStorageRuntimeException("文件复制失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath
                    + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw new FileStorageRuntimeException(
                    "文件复制失败，源文件不存在！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }

        // 复制缩略图文件
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            File srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
            if (!srcThFile.exists()) {
                throw new FileStorageRuntimeException(
                        "缩略图文件复制失败，源缩略图文件不存在！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.copyFile(srcThFile, destThFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                FileUtil.del(destThFile);
                throw new FileStorageRuntimeException(
                        "缩略图文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        File destFile = null;
        try {
            destFile = FileUtil.touch(getAbsolutePath(destFileKey));
            if (progressListener == null) {
                FileUtil.copyFile(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                InputStreamPlus in =
                        new InputStreamPlus(FileUtil.getInputStream(srcFile), progressListener, srcFile.length());
                FileUtil.writeFromStream(in, destFile);
            }
        } catch (Exception e) {
            FileUtil.del(destThFile);
            FileUtil.del(destFile);
            throw new FileStorageRuntimeException(
                    "文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }
}
