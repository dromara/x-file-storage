package cn.xuyanwu.spring.file.storage.test;

import cn.xuyanwu.spring.file.storage.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFileStorage
@MapperScan("cn.xuyanwu.spring.file.storage.test.mapper")
public class SpringFileStorageTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringFileStorageTestApplication.class, args);
	}

}
