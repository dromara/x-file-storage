package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FtpConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.move.MovePretreatment;

/**
 * FTP 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class FtpFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;
    private FileStorageClientFactory<Ftp> clientFactory;

    public FtpFileStorage(FtpConfig config, FileStorageClientFactory<Ftp> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        storagePath = config.getStoragePath();
        this.clientFactory = clientFactory;
    }

    /**
     * 获取 Client ，使用完后需要归还
     */
    public Ftp getClient() {
        return clientFactory.getClient();
    }

    /**
     * 归还 Client
     */
    public void returnClient(Ftp client) {
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
                    "文件上传失败，FTP 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件上传失败，FTP 不支持设置 Metadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }

        Ftp client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            client.upload(getAbsolutePath(basePath + fileInfo.getPath()), fileInfo.getFilename(), in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(
                        getAbsolutePath(basePath + fileInfo.getPath()),
                        fileInfo.getThFilename(),
                        new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException | IORuntimeException e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (IORuntimeException ignored) {
            }
            throw new FileStorageRuntimeException(
                    "文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Ftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.delFile(getAbsolutePath(getThFileKey(fileInfo)));
            }
            client.delFile(getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (IORuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            return client.existFile(fileInfo.getFilename());
        } catch (IORuntimeException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            FTPClient ftpClient = client.getClient();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            try (InputStream in = ftpClient.retrieveFileStream(fileInfo.getFilename())) {
                if (in == null) {
                    throw new FileStorageRuntimeException("文件下载失败，文件不存在！fileInfo：" + fileInfo);
                }
                consumer.accept(in);
                ftpClient.completePendingCommand();
            }
        } catch (IOException | IORuntimeException e) {
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
        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            FTPClient ftpClient = client.getClient();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            try (InputStream in = ftpClient.retrieveFileStream(fileInfo.getThFilename())) {
                if (in == null) {
                    throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
                }
                consumer.accept(in);
                ftpClient.completePendingCommand();
            }
        } catch (IOException | IORuntimeException e) {
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

        Ftp client = getClient();
        try {
            FTPClient ftpClient = client.getClient();
            client.cd(srcPath);

            FTPFile srcFile;
            try {
                srcFile = ftpClient.listFiles(srcFileInfo.getFilename())[0];
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
                    if (client.existFile(srcFileInfo.getFilename())) {
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
