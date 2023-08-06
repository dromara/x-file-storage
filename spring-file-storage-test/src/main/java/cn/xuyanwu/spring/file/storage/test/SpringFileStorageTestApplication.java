package cn.xuyanwu.spring.file.storage.test;

import cn.xuyanwu.spring.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFileStorage
@MapperScan("cn.xuyanwu.spring.file.storage.test.mapper")
public class SpringFileStorageTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringFileStorageTestApplication.class, args);
	}

}
