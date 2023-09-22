package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.SftpException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.SftpConfig;
import org.dromara.x.file.storage.core.ProgressInputStream;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

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

    public SftpFileStorage(SftpConfig config,FileStorageClientFactory<Sftp> clientFactory) {
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
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        if (fileInfo.getFileAcl() != null) {
            throw new FileStorageRuntimeException("文件上传失败，SFTP 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        ProgressListener listener = pre.getProgressListener();

        Sftp client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            String path = getAbsolutePath(basePath + fileInfo.getPath());
            if (!client.exist(path)) {
                client.mkDirs(path);
            }
            client.upload(path,fileInfo.getFilename(),listener == null ? in : new ProgressInputStream(in,listener,fileInfo.getSize()));

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(path,fileInfo.getThFilename(),new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (IOException | JschRuntimeException e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (JschRuntimeException ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Sftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                delFile(client,getAbsolutePath(getThFileKey(fileInfo)));
            }
            delFile(client,getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (JschRuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }

    public void delFile(Sftp client,String filename) {
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
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
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
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        } finally {
            returnClient(client);
        }
    }
}
