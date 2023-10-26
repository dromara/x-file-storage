package org.dromara.x.file.storage.fastdfs.test;

import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/23 9:58
 */
@Slf4j
@EnableFileStorage
@SpringBootApplication
public class FastDfsTestApplication implements ApplicationRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(FastDfsTestApplication.class, args);
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("💦 FastDFS test boot successful.");
    }
}