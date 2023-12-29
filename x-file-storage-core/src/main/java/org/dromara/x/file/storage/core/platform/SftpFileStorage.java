package org.dromara.x.file.storage.core.platform;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.SftpConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.move.MovePretreatment;

/**
 * SFTP 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class SftpFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;
    private FileStorageClientFactory<Sftp> clientFactory;

    public SftpFileStorage(SftpConfig config, FileStorageClientFactory<Sftp> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        storagePath = config.getStoragePath();
        this.clientFactory = clientFactory;
    }

    /**
     * 获取 Client ，使用完后需要归还
     */
    public Sftp getClient() {
        return clientFactory.getClient();
    }

    /**
     * 归还 Client
     */
    public void returnClient(Sftp client) {
        clientFactory.returnClient(client);
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    public String getFileKey(FileInfo fileInfo) {
        String basePath = fileInfo.getBasePath();
        String path = fileInfo.getPath();
        String filename = fileInfo.getFilename();

        basePath = (basePath != null) ? basePath : "";
        path = (path != null) ? path : "";
        filename = (filename != null) ? filename : "";

        return basePath + path + filename;
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        String basePath = fileInfo.getBasePath();
        String path = fileInfo.getPath();
        String filename = fileInfo.getFilename();

        basePath = (basePath != null) ? basePath : "";
        path = (path != null) ? path : "";
        filename = (filename != null) ? filename : "";

        return basePath + path + filename;
    }

    /**
     * 获取远程绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        Sftp client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            String path = getAbsolutePath(basePath + fileInfo.getPath());
            if (!client.exist(path)) {
                client.mkDirs(path);
            }
            client.upload(path, fileInfo.getFilename(), in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(path, fileInfo.getThFilename(), new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (Exception e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Sftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                delFile(client, getAbsolutePath(getThFileKey(fileInfo)));
            }
            delFile(client, getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    public void delFile(Sftp client, String filename) {
        try {
            client.delFile(filename);
        } catch (JschRuntimeException e) {
            if (!(e.getCause() instanceof SftpException && ((SftpException) e.getCause()).id == SSH_FX_NO_SUCH_FILE)) {
                throw e;
            }
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        Sftp client = getClient();
        try {
            return client.exist(getAbsolutePath(getFileKey(fileInfo)));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        } finally {
            returnClient(client);
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

        String srcPath = getAbsolutePath(srcFileInfo.getBasePath() + srcFileInfo.getPath());
        String destPath = getAbsolutePath(destFileInfo.getBasePath() + destFileInfo.getPath());
        String relativizePath =
                Paths.get(srcPath).relativize(Paths.get(destPath)).toString().replace("\\", "/") + "/";

        Sftp client = getClient();
        try {
            ChannelSftp ftpClient = client.getClient();
            client.cd(srcPath);

            SftpATTRS srcFile;
            try {
                srcFile = ftpClient.stat(srcFileInfo.getFilename());
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
            }

            // 移动缩略图文件
            String destThFileRelativizeKey = null;
            if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
                destFileInfo.setThUrl(domain + getThFileKey(destFileInfo));
                destThFileRelativizeKey = relativizePath + destFileInfo.getThFilename();
                try {
                    client.mkDirs(destPath);
                    ftpClient.rename(srcFileInfo.getThFilename(), destThFileRelativizeKey);
                } catch (Exception e) {
                    throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
                }
            }

            // 移动文件
            String destFileKey = getFileKey(destFileInfo);
            destFileInfo.setUrl(domain + destFileKey);
            String destFileRelativizeKey = relativizePath + destFileInfo.getFilename();
            try {
                ProgressListener.quickStart(pre.getProgressListener(), srcFile.getSize());
                ftpClient.rename(srcFileInfo.getFilename(), destFileRelativizeKey);
                ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getSize());
            } catch (Exception e) {
                if (destThFileRelativizeKey != null) {
                    try {
                        ftpClient.rename(destThFileRelativizeKey, srcFileInfo.getThFilename());
                    } catch (Exception ignored) {
                    }
                }
                try {
                    if (client.exist(srcFileInfo.getFilename())) {
                        client.delFile(destFileRelativizeKey);
                    } else {
                        ftpClient.rename(destFileRelativizeKey, srcFileInfo.getFilename());
                    }
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
            }
        } finally {
            returnClient(client);
        }
    }
}
