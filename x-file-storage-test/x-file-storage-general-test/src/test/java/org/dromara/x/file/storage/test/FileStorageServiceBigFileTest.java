package org.dromara.x.file.storage.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.ProgressListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;


@Slf4j
@SpringBootTest
class FileStorageServiceBigFileTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试大文件上传
     */
    @Test
    public void uploadBigFile() throws IOException {
        String url = "https://app.xuyanwu.cn/BadApple/video/Bad%20Apple.mp4";

        File file = new File(System.getProperty("java.io.tmpdir"),"Bad Apple.mp4");
        if (!file.exists()) {
            log.info("测试大文件不存在，正在下载中");
            FileUtil.writeFromStream(new URL(url).openStream(),file);
            log.info("测试大文件下载完成");
        }

        FileInfo fileInfo = fileStorageService.of(file)
                .setPath("test/")
                .setProgressMonitor(new ProgressListener() {
                    @Override
                    public void start() {
                        System.out.println("上传开始");
                    }

                    @Override
                    public void progress(long progressSize,Long allSize) {
                        if (allSize == null) {
                            System.out.println("已上传 " + progressSize + " 总大小未知");
                        } else {
                            System.out.println("已上传 " + progressSize + " 总大小" + allSize + " " + (progressSize * 10000 / allSize * 0.01) + "%");
                        }
                    }

                    @Override
                    public void finish() {
                        System.out.println("上传结束");
                    }
                })
                .upload();
        Assert.notNull(fileInfo,"大文件上传失败！");
        log.info("大文件上传成功：{}",fileInfo.toString());

        fileStorageService.delete(fileInfo);
    }

}
