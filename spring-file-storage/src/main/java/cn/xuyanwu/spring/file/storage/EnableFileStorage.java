package cn.xuyanwu.spring.file.storage;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用文件存储，会自动根据配置文件进行加载
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({FileStorageAutoConfiguration.class,FileStorageProperties.class})
public @interface EnableFileStorage {
}
