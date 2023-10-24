package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.util.Properties;

import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CHARSET;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECTION_POOL_ENABLED;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECTION_POOL_MAX_COUNT_PER_ENTRY;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECTION_POOL_MAX_IDLE_TIME;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECTION_POOL_MAX_WAIT_TIME_IN_MS;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_HTTP_ANTI_STEAL_TOKEN;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_HTTP_SECRET_KEY;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_HTTP_TRACKER_HTTP_PORT;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_TRACKER_SERVERS;

/**
 * Fast DFS 存储平台 Client 工厂
 *
 * @author XS <tonycody@qq.com>
 */
@Slf4j
@Getter
@Setter
public class FastDfsFileStorageClientFactory implements FileStorageClientFactory<StorageClient1> {
    
    
    /**
     * FastDFS 配置
     */
    private final FastDfsConfig config;
    
    /**
     * FastDFS Client
     */
    private volatile StorageClient1 client;
    
    /**
     * 构造函数，带配置参数
     *
     * @param config 配置文件
     */
    public FastDfsFileStorageClientFactory(FastDfsConfig config) {
        this.config = config;
    }
    
    /**
     * 获取 Client ，部分存储平台例如 FTP 、 SFTP 使用完后需要归还
     */
    @Override
    public StorageClient1 getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        Properties props = getProperties();
                        ClientGlobal.initByProperties(props);
                        TrackerClient trackerClient = new TrackerClient();
                        TrackerServer trackerServer = trackerClient.getTrackerServer();
                        if (trackerServer == null) {
                            throw new IllegalStateException("getConnection return null");
                        }
                        StorageServer storageServer = trackerClient.getStoreStorage(trackerServer,
                                config.getGroupName());
                        
                        if (storageServer == null) {
                            storageServer = new StorageServer(trackerServer.getInetSocketAddress().getHostName(),
                                    trackerServer.getInetSocketAddress().getPort(), 0);
                            trackerServer = null;
                        }
                        
                        if (storageServer == null) {
                            throw new IllegalStateException("getStoreStorage return null");
                        }
                        client = new StorageClient1(trackerServer, storageServer);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return client;
    }
    
    /**
     * Get the properties.
     *
     * @return {@link Properties}
     */
    @NonNull
    private Properties getProperties() {
        Properties props = new Properties();
        props.put(PROP_KEY_TRACKER_SERVERS, config.getTrackerServer());
        props.put(PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS,
                Convert.toStr(config.getConnectTimeoutInSeconds(), StrUtil.EMPTY));
        props.put(PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS,
                Convert.toStr(config.getNetworkTimeoutInSeconds(), StrUtil.EMPTY));
        props.put(PROP_KEY_CHARSET, Convert.toStr(config.getCharset(), StrUtil.EMPTY));
        props.put(PROP_KEY_HTTP_ANTI_STEAL_TOKEN, Convert.toStr(config.getHttpAntiStealToken(), StrUtil.EMPTY));
        props.put(PROP_KEY_HTTP_SECRET_KEY, Convert.toStr(config.getHttpSecretKey(), StrUtil.EMPTY));
        props.put(PROP_KEY_HTTP_TRACKER_HTTP_PORT, Convert.toStr(config.getTrackerHttpPort(), StrUtil.EMPTY));
        props.put(PROP_KEY_CONNECTION_POOL_ENABLED, Convert.toStr(config.getConnectionPoolEnabled(), StrUtil.EMPTY));
        props.put(PROP_KEY_CONNECTION_POOL_MAX_COUNT_PER_ENTRY,
                Convert.toStr(config.getConnectionPoolMaxCountPerEntry(), StrUtil.EMPTY));
        props.put(PROP_KEY_CONNECTION_POOL_MAX_IDLE_TIME,
                Convert.toStr(config.getConnectionPoolMaxIdleTime(), StrUtil.EMPTY));
        props.put(PROP_KEY_CONNECTION_POOL_MAX_WAIT_TIME_IN_MS,
                Convert.toStr(config.getConnectionPoolMaxWaitTimeInMs(), StrUtil.EMPTY));
        return props;
    }
    
    /**
     * 释放相关资源
     */
    @Override
    public void close() {
        if (client != null) {
            try {
                client.getTrackerServer().getConnection().close();
                client.setStorageServer(null);
                client.setTrackerServer(null);
                client = null;
            } catch (Exception e) {
                throw new FileStorageRuntimeException("关闭 FastDFS Storage Client 失败！", e);
            }
        }
    }
    
    /**
     * 获取平台
     */
    @Override
    public String getPlatform() {
        return config.getPlatform();
    }
    
}