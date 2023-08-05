package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpException;
import cn.hutool.extra.ftp.FtpMode;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.FtpConfig;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
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

/**
 * FTP 存储平台的 Client 工厂，使用了对象池缓存，性能更高
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class FtpFileStorageClientFactory implements FileStorageClientFactory<Ftp> {
    private String platform;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private Charset charset;
    private Long connectionTimeout;
    private Long soTimeout;
    private String serverLanguageCode;
    private String systemKey;
    private Boolean isActive;
    private GenericObjectPoolConfig<Ftp> poolConfig;
    private volatile GenericObjectPool<Ftp> pool;

    public FtpFileStorageClientFactory(FtpConfig config) {
        platform = config.getPlatform();
        host = config.getHost();
        port = config.getPort();
        user = config.getUser();
        password = config.getPassword();
        charset = config.getCharset();
        connectionTimeout = config.getConnectionTimeout();
        soTimeout = config.getSoTimeout();
        serverLanguageCode = config.getServerLanguageCode();
        systemKey = config.getSystemKey();
        isActive = config.getIsActive();
        poolConfig = config.getPool().toGenericObjectPoolConfig();
    }

    @Override
    public Ftp getClient() {
        try {
            if (pool == null) {
                synchronized (this) {
                    if (pool == null) {
                        pool = new GenericObjectPool<>(new FtpPooledObjectFactory(this),poolConfig);
                    }
                }
            }
            return pool.borrowObject();
        } catch (Exception e) {
            throw new FileStorageRuntimeException("获取 FTP Client 失败！",e);
        }
    }

    @Override
    public void returnClient(Ftp sftp) {
        try {
            pool.returnObject(sftp);
        } catch (Exception e) {
            throw new FileStorageRuntimeException("归还 FTP Client 失败！",e);
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
     * Ftp 的对象池包装的工厂
     */
    @Slf4j
    @AllArgsConstructor
    public static class FtpPooledObjectFactory extends BasePooledObjectFactory<Ftp> {
        private FtpFileStorageClientFactory factory;

        @Override
        public Ftp create() {
            if (factory == null) throw new FileStorageRuntimeException("FTP 连接失败！config 不能为空");
            try {
                return new Ftp(cn.hutool.extra.ftp.FtpConfig.create().setHost(factory.getHost()).setPort(factory.getPort())
                        .setUser(factory.getUser()).setPassword(factory.getPassword()).setCharset(factory.getCharset())
                        .setConnectionTimeout(factory.getConnectionTimeout()).setSoTimeout(factory.getSoTimeout())
                        .setServerLanguageCode(factory.getServerLanguageCode())
                        .setSystemKey(factory.getSystemKey()),factory.getIsActive() ? FtpMode.Active : FtpMode.Passive);
            } catch (Exception e) {
                throw new FileStorageRuntimeException("FTP 连接失败！platform：" + factory.getPlatform(),e);
            }
        }

        @Override
        public PooledObject<Ftp> wrap(Ftp sftp) {
            return new DefaultPooledObject<>(sftp);
        }

        @Override
        public boolean validateObject(PooledObject<Ftp> p) {
            try {
                p.getObject().cd(StrUtil.DOT);
                return true;
            } catch (FtpException e) {
                log.warn("验证 Ftp 对象失败",e);
                return false;
            }
        }

        @Override
        public void destroyObject(PooledObject<Ftp> p) {
            try {
                p.getObject().close();
            } catch (Exception e) {
                throw new FileStorageRuntimeException("销毁 Ftp 对象失败！",e);
            }
        }
    }


}
