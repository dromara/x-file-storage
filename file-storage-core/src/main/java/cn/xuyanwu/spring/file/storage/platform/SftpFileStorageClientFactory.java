package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.ftp.FtpException;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.xuyanwu.spring.file.storage.FileStorageProperties;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * SFTP 存储平台的 Client 工厂，使用了对象池缓存，性能更高
 */
@Slf4j
@Getter
@Setter
public class SftpFileStorageClientFactory implements FileStorageClientFactory<Sftp> {
    private FileStorageProperties.SFTP config;
    private GenericObjectPool<Sftp> pool;

    public SftpFileStorageClientFactory(FileStorageProperties.SFTP config) {
        this.config = config;

        GenericObjectPoolConfig<Sftp> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setTestOnBorrow(true);//取出对象前进行校验
        poolConfig.setMaxTotal(16);//最大总数量，超过此数量会进行阻塞等待
        poolConfig.setMinIdle(0);//最小空闲数量
        poolConfig.setMaxIdle(8);//最大空闲数量

        poolConfig.setTestWhileIdle(true);//开启空闲检测，用于维持连接可用和清理不可用连接
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));
//        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(10));//空闲检测间隔时间
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));
//        poolConfig.setSoftMinEvictableIdleTime();
        pool = new GenericObjectPool<>(new SftpPooledObjectFactory(this::createClient),poolConfig);
    }

    @Override
    public String getPlatform() {
        return config == null ? null : config.getPlatform();
    }

    @Override
    public Sftp getClient() {
        try {
            log.info("获取 Sftp 对象");
            return pool.borrowObject();
        } catch (Exception e) {
            throw new FileStorageRuntimeException("获取 SFTP Client 失败！",e);
        }
    }

    @Override
    public void returnClient(Sftp sftp) {
        try {
            log.info("归还 Sftp 对象");
            pool.returnObject(sftp);
        } catch (Exception e) {
            throw new FileStorageRuntimeException("归还 SFTP Client 失败！",e);
        }
    }

    @Override
    public void close() {
        pool.close();
    }

    /**
     * 仅创建
     */
    public Sftp createClient() {
        if (config == null) throw new FileStorageRuntimeException("SFTP连接失败！config 不能为空");
        Session session = null;
        try {
            if (StrUtil.isNotBlank(config.getPrivateKeyPath())) {
                //使用秘钥连接，这里手动读取 byte 进行构造用于兼容Spring的ClassPath路径、文件路径、HTTP路径等
                byte[] passphrase = StrUtil.isBlank(config.getPassword()) ? null : config.getPassword().getBytes(StandardCharsets.UTF_8);
                JSch jsch = new JSch();
                byte[] privateKey = IoUtil.readBytes(URLUtil.url(config.getPrivateKeyPath()).openStream());
                jsch.addIdentity(config.getPrivateKeyPath(),privateKey,null,passphrase);
                session = JschUtil.createSession(jsch,config.getHost(),config.getPort(),config.getUser());
                session.connect((int) config.getConnectionTimeout());
            } else {
                session = JschUtil.openSession(config.getHost(),config.getPort(),config.getUser(),config.getPassword(),(int) config.getConnectionTimeout());
            }
            return new Sftp(session,config.getCharset(),config.getConnectionTimeout());
        } catch (Exception e) {
            JschUtil.close(session);
            throw new FileStorageRuntimeException("SFTP连接失败！platform：" + config.getPlatform(),e);
        }
    }


    /**
     * Sftp 的对象池包装的工厂
     */
    @Slf4j
    @AllArgsConstructor
    public static class SftpPooledObjectFactory extends BasePooledObjectFactory<Sftp> {
        private Supplier<Sftp> sftpSupplier;

        /**
         * 创建 Sftp 对象
         */
        @Override
        public Sftp create() {
            log.info("创建 Sftp 对象");
            return sftpSupplier.get();
        }

        /**
         * 对 Sftp 进行包装
         */
        @Override
        public PooledObject<Sftp> wrap(Sftp obj) {
            log.info("包装 Sftp 对象");
            return new DefaultPooledObject<>(obj);
        }

        /**
         * 验证 Sftp
         */
        @Override
        public boolean validateObject(PooledObject<Sftp> p) {
            log.info("验证 Sftp 对象");
            try {
                p.getObject().cd(StrUtil.SLASH);
                return true;
            } catch (FtpException e) {
                return false;
            }
        }

        /**
         * 销毁 Sftp
         */
        @Override
        public void destroyObject(PooledObject<Sftp> p) {
            log.info("销毁 Sftp 对象");
            p.getObject().close();
        }
    }


}
