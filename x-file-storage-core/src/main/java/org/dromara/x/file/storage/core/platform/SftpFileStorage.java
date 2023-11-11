package org.dromara.x.file.storage.core.platform;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
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
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename();
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename();
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
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件上传失败，SFTP 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件上传失败，SFTP 不支持设置 Metadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }

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
        } catch (IOException | JschRuntimeException e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (JschRuntimeException ignored) {
            }
            throw new FileStorageRuntimeException(
                    "文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
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
        } catch (JschRuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo, e);
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
        } catch (JschRuntimeException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
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
        if (srcFileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件移动失败，不支持设置 ACL！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
        if (CollUtil.isNotEmpty(srcFileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件移动失败，不支持设置 Metadata！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw new FileStorageRuntimeException("文件移动失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath
                    + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }

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
                throw new FileStorageRuntimeException(
                        "文件移动失败，无法获取源文件信息！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo, e);
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
                    throw new FileStorageRuntimeException(
                            "缩略图文件移动失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
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
                throw new FileStorageRuntimeException(
                        "文件移动失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
        } finally {
            returnClient(client);
        }
    }
}
