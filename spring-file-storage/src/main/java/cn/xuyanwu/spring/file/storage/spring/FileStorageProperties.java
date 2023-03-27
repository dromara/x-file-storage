package cn.xuyanwu.spring.file.storage.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FileStorageProperties.class)
@ConfigurationProperties(prefix = "spring.file-storage")
public class FileStorageProperties extends cn.xuyanwu.spring.file.storage.FileStorageProperties {

}
