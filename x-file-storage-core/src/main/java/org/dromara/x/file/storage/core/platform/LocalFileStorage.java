package org.dromara.x.file.storage.core.platform;

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
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.move.MovePretreatment;

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
        Check.uploadNotSupportedAcl(platform, fileInfo, pre);
        Check.uploadNotSupportedMetadata(platform, fileInfo, pre);

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
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportedAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyNotSupportedMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

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
            if (pre.getProgressListener() == null) {
                FileUtil.copyFile(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                InputStreamPlus in = new InputStreamPlus(
                        FileUtil.getInputStream(srcFile), pre.getProgressListener(), srcFile.length());
                FileUtil.writeFromStream(in, destFile);
            }
        } catch (Exception e) {
            FileUtil.del(destThFile);
            FileUtil.del(destFile);
            throw new FileStorageRuntimeException(
                    "文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportedAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveNotSupportedMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw new FileStorageRuntimeException(
                    "文件移动失败，源文件不存在！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }

        // 移动缩略图文件
        File srcThFile = null;
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
            if (!srcThFile.exists()) {
                throw new FileStorageRuntimeException(
                        "缩略图文件移动失败，源缩略图文件不存在！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.move(srcThFile, destThFile, true);
            } catch (Exception e) {
                FileUtil.del(destThFile);
                throw new FileStorageRuntimeException(
                        "缩略图文件移动失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        File destFile = null;
        try {
            // 这里还有优化空间，例如跨存储设备或跨分区移动大文件时，会耗时很久且无法监听到进度，
            // 可以使用复制+删除源文件来实现，这样可以监听到进度。
            // 但是正常来说，不应该在单个存储平台使用的存储路径下挂载不同的分区，这会造成维护上的困难，
            // 不同的分区最好用配置成不同的存储平台，除非你明确知道为什么要这么做及可能会出现的问题。
            destFile = FileUtil.touch(getAbsolutePath(destFileKey));
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.length());
            FileUtil.move(srcFile, destFile, true);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.length());
        } catch (Exception e) {
            if (destThFile != null) {
                try {
                    FileUtil.move(destThFile, srcThFile, true);
                } catch (Exception ignored) {
                }
            }
            try {
                if (srcFile.exists()) {
                    FileUtil.del(destFile);
                } else if (destFile != null) {
                    FileUtil.move(destFile, srcFile, true);
                }
            } catch (Exception ignored) {
            }
            throw new FileStorageRuntimeException(
                    "文件移动失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }
}
