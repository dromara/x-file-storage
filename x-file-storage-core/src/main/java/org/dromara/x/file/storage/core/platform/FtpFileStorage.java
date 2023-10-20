package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FtpConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

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

    public FtpFileStorage(FtpConfig config,FileStorageClientFactory<Ftp> clientFactory) {
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
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，FTP 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，FTP 不支持设置 UserMetadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }

        Ftp client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            client.upload(getAbsolutePath(basePath + fileInfo.getPath()),fileInfo.getFilename(),in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(getAbsolutePath(basePath + fileInfo.getPath()),fileInfo.getThFilename(),new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException | IORuntimeException e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (IORuntimeException ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Ftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                client.delFile(getAbsolutePath(getThFileKey(fileInfo)));
            }
            client.delFile(getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (IORuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
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
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
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
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
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
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }
}
