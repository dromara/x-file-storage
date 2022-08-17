package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

/**
 * SFTP 存储
 */
@Getter
@Setter
public class SftpFileStorage implements FileStorage {

    /* 主机 */
    private String host;
    /* 端口，默认22 */
    private int port;
    /* 用户名 */
    private String user;
    /* 密码，默认空 */
    private String password;
    /* 私钥路径，默认空 */
    private String privateKeyPath;
    /* 编码，默认UTF-8 */
    private Charset charset;
    /* 连接超时时长，单位毫秒，默认10秒 */
    private long connectionTimeout;
    /* 存储平台 */
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;

    /**
     * 不支持单例模式运行，每次使用完了需要销毁
     */
    public Sftp getClient() {
        Session session = null;
        try {
            if (StrUtil.isNotBlank(privateKeyPath)) {
                //使用秘钥连接，这里手动读取 byte 进行构造用于兼容Spring的ClassPath路径、文件路径、HTTP路径等
                byte[] passphrase = StrUtil.isBlank(password) ? null : password.getBytes(StandardCharsets.UTF_8);
                JSch jsch = new JSch();
                byte[] privateKey = IoUtil.readBytes(URLUtil.url(privateKeyPath).openStream());
                jsch.addIdentity(privateKeyPath,privateKey,null,passphrase);
                session = JschUtil.createSession(jsch,host,port,user);
                session.connect((int) connectionTimeout);
            } else {
                session = JschUtil.openSession(host,port,user,password,(int) connectionTimeout);
            }
            return new Sftp(session,charset,connectionTimeout);
        } catch (Exception e) {
            JschUtil.close(session);
            throw new FileStorageRuntimeException("SFTP连接失败！platform：" + platform,e);
        }
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

        Sftp client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            String path = getAbsolutePath(basePath + fileInfo.getPath());
            if (!client.exist(path)) {
                client.mkDirs(path);
            }
            client.upload(path,fileInfo.getFilename(),in);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
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
            IoUtil.close(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try (Sftp client = getClient()) {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
                delFile(client,getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
            }
            delFile(client,getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
            return true;
        } catch (JschRuntimeException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
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
        try (Sftp client = getClient()) {
            return client.exist(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
        } catch (JschRuntimeException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (Sftp client = getClient();
             InputStream in = client.getClient().get(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()))) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }

        try (Sftp client = getClient(); InputStream in = client.getClient().get(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()) + fileInfo.getThFilename())) {
            consumer.accept(in);
        } catch (IOException | JschRuntimeException | SftpException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
