package cn.xuyanwu.spring.file.storage.test.config;

import cn.xuyanwu.spring.file.storage.platform.FileStorageClientFactory;
import cn.xuyanwu.spring.file.storage.platform.FtpFileStorageClientFactory;
import cn.xuyanwu.spring.file.storage.spring.SpringFileStorageProperties;
import cn.xuyanwu.spring.file.storage.spring.SpringFileStorageProperties.SpringFtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class CustomFileStorageClientFactoryConfig {


    /**
     * 自定义存储平台的 Client 工厂类
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient","cn.hutool.extra.ftp.Ftp"})
    public List<FileStorageClientFactory<?>> myFtpFileStorageClientFactory(SpringFileStorageProperties properties) {
        log.info("自定义 FTP 存储平台的 Client 工厂类");
        return properties.getFtp().stream()
                .filter(SpringFtpConfig::getEnableStorage)
                .map(FtpFileStorageClientFactory::new)
                .collect(Collectors.toList());
    }

    /**
     * 自定义存储平台的 Client 工厂类
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient","cn.hutool.extra.ftp.Ftp"})
    public List<FileStorageClientFactory<?>> myFtpFileStorageClientFactory2(SpringFileStorageProperties properties) {
        log.info("自定义 FTP 存储平台的 Client 工厂类");
        return properties.getFtp().stream()
                .filter(SpringFtpConfig::getEnableStorage)
                .map(FtpFileStorageClientFactory::new)
                .collect(Collectors.toList());
    }



}
