package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.ClientPoolHelper;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageProperties;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import cn.xuyanwu.spring.file.storage.factory.SftpClientFactory;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * SFTP 存储
 */
@Getter
@Setter
public class SftpFileStorage implements FileStorage {

    private FileStorageProperties.SFTP sftpConfig;
    /* 存储平台 */
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;

    private ClientPoolHelper<ChannelSftp, Boolean> clientPoolHelper;

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
        String filePath = basePath + fileInfo.getPath();
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);
        return clientPoolHelper.runOnce(e -> {
            try (InputStream in = pre.getFileWrapper().getInputStream()) {
                String path = getAbsolutePath(basePath + fileInfo.getPath());
                mkdirs(e, path);
                e.cd(path);
                e.put(in, fileInfo.getFilename());
                byte[] thumbnailBytes = pre.getThumbnailBytes();
                if (thumbnailBytes != null) { //上传缩略图
                    String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                    fileInfo.setThUrl(domain + newThFileKey);
                    e.put(new ByteArrayInputStream(thumbnailBytes), fileInfo.getThFilename());
                }
                return true;
            } catch (IOException ex) {
                throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), ex);
            } catch (SftpException ex) {
                throw new FileStorageRuntimeException("创建文件夹失败！platform：" + platform + "，filePath：" + filePath, ex);
            }
        }, platform);
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        return clientPoolHelper.runOnce(e -> {
            try {
                if (Objects.nonNull(fileInfo.getThFilename())) {
                    e.rm(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
                }
                e.rm(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
                return true;
            } catch (SftpException ex) {
                throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo, ex);
            }
        }, platform);
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        return clientPoolHelper.runOnce(e -> {
            try {
                e.stat(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
                return true;
            } catch (SftpException ex) {
                return false;
            }
        }, platform);
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        clientPoolHelper.runOnce(e->{
            InputStream in = null;
            try{
                e.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
                in = e.get(fileInfo.getFilename());
                if (in == null) {
                    throw new FileStorageRuntimeException("文件下载失败，文件不存在！platform：" + fileInfo);
                }
                consumer.accept(in);
            } catch (SftpException ex) {
                throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        },platform);
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        clientPoolHelper.runOnce(e->{
            InputStream in = null;
            try {
                e.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
                in = e.get(fileInfo.getThFilename());
                if (in == null) {
                    throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！platform：" + fileInfo);
                }
                consumer.accept(in);
            } catch (SftpException ex) {
                throw new FileStorageRuntimeException("缩略图文件下载失败！platform：" + fileInfo, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        },platform);
    }
    @Override
    public GenericObjectPool<ChannelSftp> clientCache() {
        return new GenericObjectPool<ChannelSftp>(new SftpClientFactory(sftpConfig), sftpConfig);
    }

    /**
     * 递归创建多级目录
     *
     * @param dir 多级目录
     */
    private void mkdirs(ChannelSftp sftp, String dir) throws SftpException {
        try {
            sftp.ls(dir);
        } catch (Exception dx) {
            String[] folders = dir.split("/");
            sftp.cd("/");
            for (String folder : folders) {
                if (folder.length() > 0) {
                    try {
                        sftp.cd(folder);
                    } catch (Exception ex) {
                        sftp.mkdir(folder);
                        sftp.cd(folder);
                    }
                }
            }
        }
    }
}
