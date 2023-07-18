package cn.xuyanwu.spring.file.storage.test.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component("fileStorageClientConfigurationSupplier")
public class FileStorageClientConfigurationSupplier {

    public Object getBosClientConfiguration() {
        log.info("获取 BosClientConfiguration");
        return new Object();
    }
}
