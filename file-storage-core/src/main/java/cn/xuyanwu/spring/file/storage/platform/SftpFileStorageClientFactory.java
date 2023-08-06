package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.ftp.FtpException;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.SftpConfig;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * SFTP 存储平台的 Client 工厂，使用了对象池缓存，性能更高
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class SftpFileStorageClientFactory implements FileStorageClientFactory<Sftp> {
    private String platform;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String privateKeyPath;
    private Charset charset;
    private Integer connectionTimeout;
    private GenericObjectPoolConfig<Sftp> poolConfig;
    private volatile GenericObjectPool<Sftp> pool;

    public SftpFileStorageClientFactory(SftpConfig config) {
        platform = config.getPlatform();
        host = config.getHost();
        port = config.getPort();
        user = config.getUser();
        password = config.getPassword();
        privateKeyPath = config.getPrivateKeyPath();
        charset = config.getCharset();
        connectionTimeout = config.getConnectionTimeout();
        poolConfig = config.getPool().toGenericObjectPoolConfig();
    }

    @Override
    public Sftp getClient() {
        try {
            if (pool == null) {
                synchronized (this) {
                    if (pool == null) {
                        pool = new GenericObjectPool<>(new SftpPooledObjectFactory(this),poolConfig);
                    }
                }
            }
            return pool.borrowObject();
        } catch (Exception e) {
            throw new FileStorageRuntimeException("获取 SFTP Client 失败！",e);
        }
    }

    @Override
    public void returnClient(Sftp sftp) {
        try {
            pool.returnObject(sftp);
        } catch (Exception e) {
            throw new FileStorageRuntimeException("归还 SFTP Client 失败！",e);
        }
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }


    /**
     * Sftp 的对象池包装的工厂
     */
    @Slf4j
    @AllArgsConstructor
    public static class SftpPooledObjectFactory extends BasePooledObjectFactory<Sftp> {
        private SftpFileStorageClientFactory factory;

        @Override
        public Sftp create() {
            Session session = null;
            try {
                if (StrUtil.isNotBlank(factory.getPrivateKeyPath())) {
                    //使用秘钥连接，这里手动读取 byte 进行构造用于兼容Spring的ClassPath路径、文件路径、HTTP路径等
                    byte[] passphrase = StrUtil.isBlank(factory.getPassword()) ? null : factory.getPassword().getBytes(StandardCharsets.UTF_8);
                    JSch jsch = new JSch();
                    byte[] privateKey = IoUtil.readBytes(URLUtil.url(factory.getPrivateKeyPath()).openStream());
                    jsch.addIdentity(factory.getPrivateKeyPath(),privateKey,null,passphrase);
                    session = JschUtil.createSession(jsch,factory.getHost(),factory.getPort(),factory.getUser());
                    session.connect(factory.getConnectionTimeout());
                } else {
                    session = JschUtil.openSession(factory.getHost(),factory.getPort(),factory.getUser(),factory.getPassword(),factory.getConnectionTimeout());
                }
                return new Sftp(session,factory.getCharset(),factory.getConnectionTimeout());
            } catch (Exception e) {
                JschUtil.close(session);
                throw new FileStorageRuntimeException("SFTP 连接失败！platform：" + factory.getPlatform(),e);
            }
        }

        @Override
        public PooledObject<Sftp> wrap(Sftp sftp) {
            return new DefaultPooledObject<>(sftp);
        }

        @Override
        public boolean validateObject(PooledObject<Sftp> p) {
            try {
                p.getObject().cd(StrUtil.DOT);
                return true;
            } catch (FtpException e) {
                log.warn("验证 Sftp 对象失败",e);
                return false;
            }
        }

        @Override
        public void destroyObject(PooledObject<Sftp> p) {
            p.getObject().close();
        }
    }


}
