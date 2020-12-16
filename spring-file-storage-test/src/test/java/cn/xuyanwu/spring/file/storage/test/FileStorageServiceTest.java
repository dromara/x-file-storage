package cn.xuyanwu.spring.file.storage.test;

import cn.hutool.core.lang.Assert;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;


@Slf4j
@SpringBootTest
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 单独对文件上传进行测试
     */
    @Test
    public void upload() {

        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        FileInfo fileInfo = fileStorageService.of(in).setName("file").setOriginalFilename(filename).setPath("test/").thumbnail().upload();
        Assert.notNull(fileInfo,"文件上传失败！");
        log.info("文件上传成功：{}",fileInfo.toString());
    }

    /**
     * 测试根据 url 上传文件
     */
    @Test
    public void uploadByURL() throws MalformedURLException {

        URL url = new URL("https://www.xuyanwu.cn/file/upload/1566046282790-1.png");

        FileInfo fileInfo = fileStorageService.of(url).thumbnail().setOriginalFilename("1566046282790-1.png").setPath("test/").setObjectId("0").setObjectType("0").upload();
        Assert.notNull(fileInfo,"文件上传失败！");
        log.info("文件上传成功：{}",fileInfo.toString());
    }

    /**
     * 测试上传并删除文件
     */
    @Test
    public void delete() {
        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        FileInfo fileInfo = fileStorageService.of(in).setOriginalFilename(filename).setPath("test/").setObjectId("0").setObjectType("0").upload();
        Assert.notNull(fileInfo,"文件上传失败！");
        boolean delete = fileStorageService.delete(fileInfo.getUrl());
        Assert.isTrue(delete,"文件删除失败！" + fileInfo.getUrl());
        log.info("文件删除成功：{}",fileInfo.toString());
    }

}
