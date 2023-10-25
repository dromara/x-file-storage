package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig.FastDfsExtra;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig.FastDfsStorageServer;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig.FastDfsTrackerServer;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
import static org.dromara.x.file.storage.core.constant.Regex.IP_COLON_PORT;
import static org.dromara.x.file.storage.core.constant.Regex.IP_COLON_PORT_COMMA;

/**
 * Fast DFS 存储平台 Client 工厂
 *
 * @author XS <tonycody@qq.com>
 */
@Slf4j
@Getter
@Setter
public class FastDfsFileStorageClientFactory implements FileStorageClientFactory<StorageClient> {
    
    
    /**
     * FastDFS 配置
     */
    private final FastDfsConfig config;
    
    /**
     * FastDFS Client
     */
    private volatile StorageClient client;
    
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
    public StorageClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        if (config.getTrackerServer() == null && config.getStorageServer() == null) {
                            throw new FileStorageRuntimeException("Tracker server 或 Storage server 未配置。");
                        }
                        
                        // 优先通过 Tracker server 获取客户端
                        if (config.getTrackerServer() != null) {
                            client = getClientByTrackerServer();
                        } else {
                            // 仅使用 Storage server
                            client = getClientByStorage();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return client;
    }
    
    /**
     * 使用 Tracker server 模式
     *
     * @return {@link StorageClient}
     * @throws MyException
     * @throws IOException
     */
    private StorageClient getClientByTrackerServer() throws MyException, IOException {
        Assert.notNull(config.getTrackerServer(), "Tracker server 配置为空");
        Assert.isTrue(ReUtil.isMatch(IP_COLON_PORT_COMMA, config.getTrackerServer().getServerAddr()),
                "Tracker server 配置错误");
        Properties props = getProperties();
        ClientGlobal.initByProperties(props);
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getTrackerServer();
        StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
        return new StorageClient(trackerServer, storeStorage);
    }
    
    /**
     * 仅使用 Storage server 模式
     *
     * @return {@link StorageClient}
     * @throws IOException
     */
    private StorageClient getClientByStorage() throws IOException {
        FastDfsStorageServer storageServer = config.getStorageServer();
        Assert.notNull(storageServer, "Storage server 配置为空");
        Assert.isTrue(ReUtil.isMatch(IP_COLON_PORT, storageServer.getServerAddr()), "Storage server 配置错误");
        initProp();
        List<String> split = StrUtil.split(storageServer.getServerAddr(), StrPool.C_COLON);
        return new StorageClient(null,
                new StorageServer(split.get(0), Integer.parseInt(split.get(1)), storageServer.getStorePath()));
    }
    
    /**
     * Storage init properties
     */
    private void initProp() {
        Properties props = getProperties();
        String connectTimeoutInSecondsConf = props.getProperty(PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS);
        String networkTimeoutInSecondsConf = props.getProperty(PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS);
        String charsetConf = props.getProperty(PROP_KEY_CHARSET);
        String httpAntiStealTokenConf = props.getProperty(PROP_KEY_HTTP_ANTI_STEAL_TOKEN);
        String httpSecretKeyConf = props.getProperty(PROP_KEY_HTTP_SECRET_KEY);
        String httpTrackerHttpPortConf = props.getProperty(PROP_KEY_HTTP_TRACKER_HTTP_PORT);
        String poolEnabled = props.getProperty(PROP_KEY_CONNECTION_POOL_ENABLED);
        String poolMaxCountPerEntry = props.getProperty(PROP_KEY_CONNECTION_POOL_MAX_COUNT_PER_ENTRY);
        String poolMaxIdleTime = props.getProperty(PROP_KEY_CONNECTION_POOL_MAX_IDLE_TIME);
        String poolMaxWaitTimeInMS = props.getProperty(PROP_KEY_CONNECTION_POOL_MAX_WAIT_TIME_IN_MS);
        if (connectTimeoutInSecondsConf != null && !connectTimeoutInSecondsConf.trim().isEmpty()) {
            ClientGlobal.g_connect_timeout = Integer.parseInt(connectTimeoutInSecondsConf.trim()) * 1000;
        }
        if (networkTimeoutInSecondsConf != null && !networkTimeoutInSecondsConf.trim().isEmpty()) {
            ClientGlobal.g_network_timeout = Integer.parseInt(networkTimeoutInSecondsConf.trim()) * 1000;
        }
        if (charsetConf != null && !charsetConf.trim().isEmpty()) {
            ClientGlobal.g_charset = charsetConf.trim();
        }
        if (httpAntiStealTokenConf != null && !httpAntiStealTokenConf.trim().isEmpty()) {
            ClientGlobal.g_anti_steal_token = Boolean.parseBoolean(httpAntiStealTokenConf);
        }
        if (httpSecretKeyConf != null && !httpSecretKeyConf.trim().isEmpty()) {
            ClientGlobal.g_secret_key = httpSecretKeyConf.trim();
        }
        if (httpTrackerHttpPortConf != null && !httpTrackerHttpPortConf.trim().isEmpty()) {
            ClientGlobal.g_tracker_http_port = Integer.parseInt(httpTrackerHttpPortConf);
        }
        if (poolEnabled != null && !poolEnabled.trim().isEmpty()) {
            ClientGlobal.g_connection_pool_enabled = Boolean.parseBoolean(poolEnabled);
        }
        if (poolMaxCountPerEntry != null && !poolMaxCountPerEntry.trim().isEmpty()) {
            ClientGlobal.g_connection_pool_max_count_per_entry = Integer.parseInt(poolMaxCountPerEntry);
        }
        if (poolMaxIdleTime != null && !poolMaxIdleTime.trim().isEmpty()) {
            ClientGlobal.g_connection_pool_max_idle_time = Integer.parseInt(poolMaxIdleTime) * 1000;
        }
        if (poolMaxWaitTimeInMS != null && !poolMaxWaitTimeInMS.trim().isEmpty()) {
            ClientGlobal.g_connection_pool_max_wait_time_in_ms = Integer.parseInt(poolMaxWaitTimeInMS);
        }
    }
    
    /**
     * Get the properties.
     *
     * @return {@link Properties}
     */
    @NonNull
    private Properties getProperties() {
        Properties props = new Properties();
        if (config.getTrackerServer() != null) {
            FastDfsTrackerServer trackerServer = config.getTrackerServer();
            props.put(PROP_KEY_TRACKER_SERVERS, trackerServer.getServerAddr());
            props.put(PROP_KEY_HTTP_TRACKER_HTTP_PORT, Convert.toStr(trackerServer.getHttpPort(), StrUtil.EMPTY));
        }
        
        if (config.getExtra() != null) {
            FastDfsExtra extra = config.getExtra();
            props.put(PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS,
                    Convert.toStr(extra.getConnectTimeoutInSeconds(), StrUtil.EMPTY));
            props.put(PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS,
                    Convert.toStr(extra.getNetworkTimeoutInSeconds(), StrUtil.EMPTY));
            props.put(PROP_KEY_CHARSET, Convert.toStr(extra.getCharset(), StrUtil.EMPTY));
            props.put(PROP_KEY_HTTP_ANTI_STEAL_TOKEN, Convert.toStr(extra.getHttpAntiStealToken(), StrUtil.EMPTY));
            props.put(PROP_KEY_HTTP_SECRET_KEY, Convert.toStr(extra.getHttpSecretKey(), StrUtil.EMPTY));
            props.put(PROP_KEY_CONNECTION_POOL_ENABLED, Convert.toStr(extra.getConnectionPoolEnabled(), StrUtil.EMPTY));
            props.put(PROP_KEY_CONNECTION_POOL_MAX_COUNT_PER_ENTRY,
                    Convert.toStr(extra.getConnectionPoolMaxCountPerEntry(), StrUtil.EMPTY));
            props.put(PROP_KEY_CONNECTION_POOL_MAX_IDLE_TIME,
                    Convert.toStr(extra.getConnectionPoolMaxIdleTime(), StrUtil.EMPTY));
            props.put(PROP_KEY_CONNECTION_POOL_MAX_WAIT_TIME_IN_MS,
                    Convert.toStr(extra.getConnectionPoolMaxWaitTimeInMs(), StrUtil.EMPTY));
        }
        
        return props;
    }
    
    /**
     * 释放相关资源
     */
    @Override
    public void close() {
        if (client != null) {
            try {
                connClose(client.getTrackerServer());
                connClose(client.getStorageServer());
                client.setStorageServer(null);
                client.setTrackerServer(null);
                client = null;
            } catch (Exception e) {
                throw new FileStorageRuntimeException("关闭 FastDFS Storage Client 失败！", e);
            }
        }
    }
    
    /**
     * @param trackerServer
     */
    private void connClose(TrackerServer trackerServer) {
        Optional.ofNullable(trackerServer).ifPresent(e -> {
            try {
                e.getConnection().close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
    
    /**
     * 获取平台
     */
    @Override
    public String getPlatform() {
        return config.getPlatform();
    }
    
}