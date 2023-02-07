package cn.xuyanwu.spring.file.storage.factory;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.xuyanwu.spring.file.storage.FileStorageProperties;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SftpClientFactory extends BasePooledObjectFactory<ChannelSftp> {
    private FileStorageProperties.SFTP config;

    public SftpClientFactory(FileStorageProperties.SFTP config) {
        this.config = config;
    }

    @Override
    public ChannelSftp create() throws JSchException, IOException {
        JSch jsch = new JSch();
        Session sshSession;
        String privateKeyPath = config.getPrivateKeyPath();
        String password = config.getPassword();
        int port = config.getPort();
        String user = config.getUser();
        String host = config.getHost();
        if (StrUtil.isNotBlank(privateKeyPath)) {
            //使用秘钥连接，这里手动读取 byte 进行构造用于兼容Spring的ClassPath路径、文件路径、HTTP路径等
            byte[] passphrase = StrUtil.isBlank(password) ? null : password.getBytes(StandardCharsets.UTF_8);
            byte[] privateKey = IoUtil.readBytes(URLUtil.url(privateKeyPath).openStream());
            jsch.addIdentity(privateKeyPath, privateKey, null, passphrase);
            sshSession = jsch.getSession(user, host, port);
        } else {
            sshSession = jsch.getSession(user, host, port);
            sshSession.setPassword(password);
        }
        sshSession.setConfig("StrictHostKeyChecking", "no");
        sshSession.connect((int) config.getConnectionTimeout());
        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        channel.connect();
        return channel;
    }

    @Override
    public PooledObject<ChannelSftp> wrap(ChannelSftp channelSftp) {
        return new DefaultPooledObject<>(channelSftp);
    }

    /**
     * 销毁对象
     *
     * @param p
     * @return
     * @author fengfan
     * @date 2022/1/14 15:26
     */
    @Override
    public void destroyObject(PooledObject<ChannelSftp> p) {
        ChannelSftp channelSftp = p.getObject();
        channelSftp.disconnect();
    }

    /**
     * 激活连接池里面的sftp连接
     *
     * @param p
     * @throws Exception
     */
    @Override
    public void activateObject(PooledObject<ChannelSftp> p) throws Exception {
        ChannelSftp channelSftp = p.getObject();
        if (!channelSftp.isConnected()) {
            channelSftp.connect();
        }

    }
}