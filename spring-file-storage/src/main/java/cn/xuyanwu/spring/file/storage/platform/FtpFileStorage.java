package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.ClientPoolHelper;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageProperties;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import cn.xuyanwu.spring.file.storage.factory.FtpClientFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * FTP 存储
 */
@Getter
@Setter
public class FtpFileStorage implements FileStorage {

    private FileStorageProperties.FTP ftpConfig;


    /* 存储平台 */
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;

    private ClientPoolHelper<FTPClient,Boolean> clientPoolHelper;

    @Override
    public GenericObjectPool<FTPClient> clientCache() {
        return new GenericObjectPool<FTPClient>(new FtpClientFactory(ftpConfig), ftpConfig);
    }

    @Override
    public void close() {

    }

    /**
     * 获取远程绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);
        clientPoolHelper.runOnce(platform,e->{
            try (InputStream in = pre.getFileWrapper().getInputStream()) {
                String dir = getAbsolutePath(basePath + fileInfo.getPath());
                e.mkd(dir);
                e.changeWorkingDirectory(dir);
                e.storeFile(fileInfo.getFilename(), in);
                byte[] thumbnailBytes = pre.getThumbnailBytes();
                if (thumbnailBytes != null) { //上传缩略图
                    String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                    fileInfo.setThUrl(domain + newThFileKey);
                    e.storeFile(fileInfo.getThFilename(), new ByteArrayInputStream(thumbnailBytes));
                }
            } catch (IOException ex) {
                throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), ex);
            }
        });
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        return clientPoolHelper.runOnce(platform,client -> {
            try {
                return client.deleteFile(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return clientPoolHelper.runOnce(platform,client -> {
            try {
                String absolutePath = getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename());
                String[] names = client.listNames(absolutePath);
                return names.length > 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        clientPoolHelper.runOnce(platform,client -> {
            try(InputStream in= client.retrieveFileStream(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));){
                if (in == null || client.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
                    return;
                }
                consumer.accept(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                try {
                    client.completePendingCommand();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        clientPoolHelper.runOnce(client -> {
            try(InputStream in= client.retrieveFileStream(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()))){
                if (in == null || client.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
                    return;
                }
                consumer.accept(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    client.completePendingCommand();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        },platform);
    }
}