package org.dromara.x.file.storage.fastdfs.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerGroup;
import org.csource.fastdfs.TrackerServer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/24 7:29
 */
@Slf4j
class FastDfsClientTests {
    
    @Test
    void clientTest() throws MyException, IOException {
        File file = FileUtil.file("test.txt");
        String fileId = getStorageClient(true).upload_file1(FileUtil.readBytes(file), FileUtil.extName(file), null);
        Console.log(fileId);
    }
    
    StorageClient1 getStorageClient(boolean onlyStorage) {
        TrackerServer trackerServer = null;
        StorageServer storageServer = null;
        try {
            TrackerClient trackerClient = getTrackerClient();
            trackerServer = trackerClient.getTrackerServer();
            if (onlyStorage) {
                storageServer = new StorageServer(trackerServer.getInetSocketAddress().getHostName(),
                        trackerServer.getInetSocketAddress().getPort(), 0);
            } else {
                storageServer = trackerClient.getStoreStorage(trackerServer);
            }
            return new StorageClient1(trackerServer, storageServer);
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
        // 直连tracker
        ClientGlobal.setG_secret_key("FastDFS1234567890");
        ClientGlobal.setG_connect_timeout(5 * 1000);
        ClientGlobal.setG_network_timeout(30 * 1000);
        ClientGlobal.setG_charset("UTF-8");
        ClientGlobal.setG_anti_steal_token(false);
        String trackerServer = "114.118.2.198:23000";
        try {
            String[] szTrackerServers = trackerServer.split(";");
            InetSocketAddress[] trackerServers = new InetSocketAddress[szTrackerServers.length];
            for (int i = 0; i < szTrackerServers.length; i++) {
                String[] parts = szTrackerServers[i].split(StrPool.COLON, 2);
                trackerServers[i] = new InetSocketAddress(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            }
            ClientGlobal.setG_tracker_group(new TrackerGroup(trackerServers));
            return new TrackerClient(ClientGlobal.g_tracker_group);
        } catch (Exception e) {
            log.error(StrUtil.format("无法连接TrackerClient:ex={}", e.getMessage()), e);
        }
        
        return null;
    }
}