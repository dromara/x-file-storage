package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * FTP 存储
 */
@Getter
@Setter
public class FtpFileStorage implements FileStorage {

    /* 主机 */
    private String host;
    /* 端口，默认21 */
    private int port;
    /* 用户名，默认 anonymous（匿名） */
    private String user;
    /* 密码，默认空 */
    private String password;
    /* 编码，默认UTF-8 */
    private Charset charset;
    /* 连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setConnectTimeout(int)} */
    private long connectionTimeout;
    /* Socket连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setSoTimeout(int)} */
    private long soTimeout;
    /* 设置服务器语言，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#setServerLanguageCode(String)} */
    private String serverLanguageCode;
    /**
     * 服务器标识，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#FTPClientConfig(String)}
     * 例如：org.apache.commons.net.ftp.FTPClientConfig.SYST_NT
     */
    private String systemKey;
    /* 是否主动模式，默认被动模式 */
    private Boolean isActive = false;
    /* 存储平台 */
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;

    /**
     * 不支持单例模式运行，每次使用完了需要销毁
     */
    public Ftp getClient() {
        FtpConfig config = FtpConfig.create().setHost(host).setPort(port).setUser(user).setPassword(password).setCharset(charset)
                .setConnectionTimeout(connectionTimeout).setSoTimeout(soTimeout).setServerLanguageCode(serverLanguageCode)
                .setSystemKey(systemKey);
        return new Ftp(config,isActive ? FtpMode.Active : FtpMode.Passive);
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
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        Ftp client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            client.upload(getAbsolutePath(basePath + fileInfo.getPath()),fileInfo.getFilename(),in);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
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
            IoUtil.close(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try (Ftp client = getClient()) {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                client.delFile(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
            }
            client.delFile(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
            return true;
        } catch (IOException | IORuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
        }
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        try (Ftp client = getClient()) {
            return client.existFile(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
        } catch (IOException | IORuntimeException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (Ftp client = getClient()) {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            try (InputStream in = client.getClient().retrieveFileStream(fileInfo.getFilename())) {
                if (in == null) {
                    throw new FileStorageRuntimeException("文件下载失败，文件不存在！platform：" + fileInfo);
                }
                consumer.accept(in);
            }
        } catch (IOException | IORuntimeException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }

        try (Ftp client = getClient()) {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            try (InputStream in = client.getClient().retrieveFileStream(fileInfo.getThFilename())) {
                if (in == null) {
                    throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！platform：" + fileInfo);
                }
                consumer.accept(in);
            }
        } catch (IOException | IORuntimeException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
