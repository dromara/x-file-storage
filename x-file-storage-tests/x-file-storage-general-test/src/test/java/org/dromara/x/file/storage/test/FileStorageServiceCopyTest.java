package org.dromara.x.file.storage.test;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FileStorageServiceCopyTest {

    @Autowired
    private FileStorageService fileStorageService;

    private FileInfo upload() {
        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        FileInfo fileInfo = fileStorageService
                .of(in)
                .setOriginalFilename(filename)
                .setPath("test/")
                .setSaveFilename("aaa.jpg")
                .setSaveThFilename("bbb")
                .thumbnail(200, 200)
                .upload();
        Assert.notNull(fileInfo, "文件上传失败！");
        log.info("被复制的文件上传成功：{}", fileInfo);

        // 为了防止有些存储平台（例如又拍云）刚上传完后就进行操作会出现错误，这里等待一会
        ThreadUtil.sleep(1000);
        return fileInfo;
    }

    /**
     * 测试复制到不同路径下
     */
    @Test
    public void path() {
        FileInfo fileInfo = upload();
        log.info("测试复制到其它路径下：{}", fileInfo);
        FileInfo destFileInfo =
                fileStorageService.copy(fileInfo).setPath("copy/").copy();
        log.info("测试复制到其它路径下完成：{}", destFileInfo);
    }

    /**
     * 测试复制到同路径下同文件名
     */
    @Test
    public void filename() {
        FileInfo fileInfo = upload();
        log.info("测试复制到同路径下且带进度监听：{}", fileInfo);
        FileInfo destFileInfo = fileStorageService
                .copy(fileInfo)
                .setFilename("aaaCopy.jpg")
                .setThFilename("aaaCopy.min.jpg")
                .setProgressListener((progressSize, allSize) ->
                        log.info("文件复制进度：{} {}%", progressSize, progressSize * 100 / allSize))
                .copy();
        log.info("测试复制到同路径下且带进度监听完成：{}", destFileInfo);
    }

    /**
     * 测试跨平台复制
     */
    @Test
    public void cross() {
        FileInfo fileInfo = upload();
        log.info("测试复制到其它存储平台下：{}", fileInfo);
        FileInfo destFileInfo = fileStorageService
                .copy(fileInfo)
                .setPlatform("local-plus-1")
                .setProgressListener((progressSize, allSize) ->
                        log.info("文件复制进度：{} {}%", progressSize, progressSize * 100 / allSize))
                .copy();
        log.info("测试复制到其它存储平台下完成：{}", destFileInfo);
    }
}
