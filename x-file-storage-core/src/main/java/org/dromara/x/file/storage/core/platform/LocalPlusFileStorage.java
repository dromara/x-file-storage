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
import org.dromara.x.file.storage.core.FileStorageProperties.LocalPlusConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.move.MovePretreatment;

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
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        try {
            File newFile = FileUtil.touch(getAbsolutePath(newFileKey));
            FileWrapper fileWrapper = pre.getFileWrapper();
            if (fileWrapper.supportTransfer()) { // 移动文件，速度较快
                ProgressListener.quickStart(pre.getProgressListener(), fileWrapper.getSize());
                fileWrapper.transferTo(newFile);
                ProgressListener.quickFinish(pre.getProgressListener(), fileWrapper.getSize());
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
            try {
                FileUtil.del(getAbsolutePath(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                FileUtil.del(getAbsolutePath(getThFileKey(fileInfo)));
            }
            return FileUtil.del(getAbsolutePath(getFileKey(fileInfo)));
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return new File(getAbsolutePath(getFileKey(fileInfo))).exists();
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
        }

        // 复制缩略图文件
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                File srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.copyFile(srcThFile, destThFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                try {
                    FileUtil.del(destThFile);
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
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
            try {
                FileUtil.del(destThFile);
            } catch (Exception ignored) {
            }
            try {
                FileUtil.del(destFile);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, null);
        }

        // 移动缩略图文件
        File srcThFile = null;
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.move(srcThFile, destThFile, true);
            } catch (Exception e) {
                try {
                    FileUtil.del(destThFile);
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
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
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
