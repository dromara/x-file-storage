package cn.xuyanwu.spring.file.storage.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import cn.xuyanwu.spring.file.storage.ProgressListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


@Slf4j
@SpringBootTest
class FileStorageServicePoolTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试存储平台的对象池
     */
    @Test
    public void pool() throws InterruptedException {
        log.info("开始尝试第一次验证");
        upload();
        log.info("第一次验证成功");

        log.info("等待 3 分钟后检查再次进行尝试");
        Thread.sleep(60 * 1000);
        log.info("等待 2 分钟后检查再次进行尝试");
        Thread.sleep(60 * 1000);
        log.info("等待 1 分钟后检查再次进行尝试");
        Thread.sleep(60 * 1000);

        log.info("开始尝试第二次验证");
        upload();
        log.info("第二次验证成功");

    }

    public void upload() {
        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
        FileInfo fileInfo = fileStorageService.of(in).setOriginalFilename(filename).upload();
        Assert.notNull(fileInfo,"文件上传失败！");

        log.info("尝试删除已存在的文件：{}",fileInfo);
        boolean delete = fileStorageService.delete(fileInfo.getUrl());
        Assert.isTrue(delete,"文件删除失败！" + fileInfo.getUrl());
        log.info("文件删除成功：{}",fileInfo);
    }

}
