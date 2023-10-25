package org.dromara.x.file.storage.fastdfs.test;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CHARSET;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_HTTP_ANTI_STEAL_TOKEN;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_HTTP_SECRET_KEY;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS;
import static org.csource.fastdfs.ClientGlobal.PROP_KEY_TRACKER_SERVERS;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/24 7:29
 */
@Slf4j
class FastDfsClientTests {
    
    private static final String TRACKER_SERVER = "172.28.133.14:22122";
    
    private static final String FASTDFS_IP_ADDR = "172.28.133.14";
    
    @Test
    void clientTest() throws MyException, IOException {
        File file = FileUtil.file("fastdfs.txt");
        String[] strings = getStorageClient(true).upload_file(FileUtil.readBytes(file), FileUtil.extName(file), null);
        Console.log(JSONUtil.toJsonPrettyStr(strings));
    }
    
    StorageClient getStorageClient(boolean onlyStorage) {
        TrackerServer trackerServer;
        StorageServer storageServer;
        try {
            TrackerClient trackerClient = getTrackerClient();
            trackerServer = trackerClient.getTrackerServer();
            if (onlyStorage) {
                trackerServer = null;
                storageServer = new StorageServer(FASTDFS_IP_ADDR, 23000, 0);
            } else {
                storageServer = trackerClient.getStoreStorage(trackerServer);
            }
            return new StorageClient(trackerServer, storageServer);
        } catch (Exception e) {
            log.error(StrUtil.format("无法连接服务器:ex={}", e.getMessage()), e);
        }
        return null;
    }
    
    /**
     * 初始化 Tracker Client
     *
     * @return {@link TrackerClient}
     * @throws Exception
     */
    TrackerClient getTrackerClient() {
        try {
            ClientGlobal.initByProperties(getProperties());
            return new TrackerClient();
        } catch (Exception e) {
            log.error(StrUtil.format("无法连接TrackerClient:ex={}", e.getMessage()), e);
        }
        
        return null;
    }
    
    /**
     * @return {@link Properties}
     */
    private Properties getProperties() {
        Properties props = new Properties();
        props.put(PROP_KEY_TRACKER_SERVERS, TRACKER_SERVER);
        props.put(PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS, Convert.toStr(5 * 1000, StrUtil.EMPTY));
        props.put(PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS, Convert.toStr(30 * 1000, StrUtil.EMPTY));
        props.put(PROP_KEY_CHARSET, "UTF-8");
        props.put(PROP_KEY_HTTP_ANTI_STEAL_TOKEN, Convert.toStr(false, StrUtil.EMPTY));
        props.put(PROP_KEY_HTTP_SECRET_KEY, "FastDFS1234567890");
        return props;
    }
}