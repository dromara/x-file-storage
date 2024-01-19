package org.dromara.x.file.storage.test.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageProperties.HuaweiObsConfig;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.platform.FtpFileStorageClientFactory;
import org.dromara.x.file.storage.core.platform.HuaweiObsFileStorage;
import org.dromara.x.file.storage.spring.SpringFileStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 自定义存储平台设置
 */
@Slf4j
@Component
public class CustomFileStorage {

    /**
     * 华为云 OBS 存储 Bean ，注意返回值必须是个 List
     */
    @Bean
    public List<HuaweiObsFileStorage> myHuaweiObsFileStorageList() {
        HuaweiObsConfig config = new HuaweiObsConfig();
        config.setPlatform("my-huawei-obs-1");
        config.setAccessKey("");
        config.setSecretKey("");
        config.setEndPoint("");
        config.setBucketName("");
        config.setDomain("");
        config.setBasePath("");

        // TODO 其它更多配置
        return FileStorageServiceBuilder.buildHuaweiObsFileStorage(Collections.singletonList(config), null);
    }

    /**
     * 自定义存储平台的 Client 工厂类
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient", "cn.hutool.extra.ftp.Ftp"})
    public List<FileStorageClientFactory<?>> myFtpFileStorageClientFactory(SpringFileStorageProperties properties) {
        log.info("自定义 FTP 存储平台的 Client 工厂类");
        return properties.getFtp().stream()
                .filter(SpringFileStorageProperties.SpringFtpConfig::getEnableStorage)
                .map(FtpFileStorageClientFactory::new)
                .collect(Collectors.toList());
    }

    /**
     * 自定义存储平台的 Client 工厂类
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient", "cn.hutool.extra.ftp.Ftp"})
    public List<FileStorageClientFactory<?>> myFtpFileStorageClientFactory2(SpringFileStorageProperties properties) {
        log.info("自定义 FTP 存储平台的 Client 工厂类");
        return properties.getFtp().stream()
                .filter(SpringFileStorageProperties.SpringFtpConfig::getEnableStorage)
                .map(FtpFileStorageClientFactory::new)
                .collect(Collectors.toList());
    }

    /**
     * 自定义存储平台的 Client 工厂类，注意返回值必须是个 List
     */
    //    @Bean
    public List<FileStorageClientFactory<?>> myHuaweiObsFileStorageClientFactory(
            SpringFileStorageProperties properties) {
        return properties.getHuaweiObs().stream()
                .filter(SpringFileStorageProperties.SpringHuaweiObsConfig::getEnableStorage)
                .map(config -> new FileStorageClientFactory<ObsClient>() {
                    private volatile ObsClient client;

                    @Override
                    public String getPlatform() {
                        return config.getPlatform();
                    }

                    @Override
                    public ObsClient getClient() {
                        if (client == null) {
                            synchronized (this) {
                                if (client == null) {
                                    log.info("初始化自定义 华为云 OBS Client {}", config.getPlatform());
                                    ObsConfiguration obsConfig = new ObsConfiguration();
                                    // 设置网络代理或其它自定义操作
                                    Map<String, Object> attr = config.getAttr();
                                    String address = MapUtil.getStr(attr, "address");
                                    Integer port = MapUtil.getInt(attr, "port");
                                    String username = MapUtil.getStr(attr, "username");
                                    String password = MapUtil.getStr(attr, "password");
                                    obsConfig.setHttpProxy(address, port, username, password);
                                    client = new ObsClient(
                                            config.getAccessKey(),
                                            config.getSecretKey(),
                                            config.getEndPoint(),
                                            obsConfig);
                                }
                            }
                        }
                        return client;
                    }

                    @Override
                    public void close() {
                        IoUtil.close(client);
                        client = null;
                    }
                })
                .collect(Collectors.toList());
    }
}
