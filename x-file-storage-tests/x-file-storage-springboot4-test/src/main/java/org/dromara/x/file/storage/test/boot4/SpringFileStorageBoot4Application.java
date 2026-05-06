package org.dromara.x.file.storage.test.boot4;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFileStorage
@MapperScan("org.dromara.x.file.storage.test.boot4.mapper")
public class SpringFileStorageBoot4Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringFileStorageBoot4Application.class, args);
    }
}
