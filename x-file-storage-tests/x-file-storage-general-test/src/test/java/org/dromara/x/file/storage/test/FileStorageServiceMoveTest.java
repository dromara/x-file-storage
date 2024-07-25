package org.dromara.x.file.storage.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FileStorageServiceMoveTest {
    /**
     * 测试时使用大文件
     */
    private final boolean useBigFile = false;

    @Autowired
    private FileStorageService fileStorageService;

    private FileInfo upload() {
        return useBigFile ? uploadBigFile() : uploadSmallFile();
    }

    private FileInfo uploadSmallFile() {
        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        FileInfo fileInfo = fileStorageService
                .of(in)
                .setOriginalFilename(filename)
                .setPath("test/")
                .setSaveFilename("aaa.jpg")
                .setSaveThFilename("bbb")
                .thumbnail(200, 200)
                .setAcl(Constant.ACL.PUBLIC_READ)
                .putMetadata(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadFileName.jpg")
                .putThMetadata(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadThFileName.jpg")
                .putUserMetadata("aaa", "111")
                .putThUserMetadata("bbb", "222")
                .upload();
        Assert.notNull(fileInfo, "文件上传失败！");
        log.info("被移动的文件上传成功：{}", fileInfo);

        // 为了防止有些存储平台（例如又拍云）刚上传完后就进行操作会出现错误，这里等待一会
        ThreadUtil.sleep(1000);
        return fileInfo;
    }

    @SneakyThrows
    private FileInfo uploadBigFile() {

        String url = "https://app.xuyanwu.cn/BadApple/video/Bad%20Apple.mp4";
        File file = new File(System.getProperty("java.io.tmpdir"), "Bad Apple.mp4");
        if (!file.exists()) {
            log.info("测试大文件不存在，正在下载中");
            FileUtil.writeFromStream(new URL(url).openStream(), file);
            log.info("测试大文件下载完成");
        }

        InputStream thIn = this.getClass().getClassLoader().getResourceAsStream("image.jpg");
        FileInfo fileInfo = fileStorageService
                .of(file)
                .thumbnailOf(thIn)
                .setPath("test/")
                .setSaveFilename("aaa.mp4")
                .setSaveThFilename("bbb")
                .thumbnail(200, 200)
                .setAcl(Constant.ACL.PUBLIC_READ)
                .putMetadata(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadFileName.mp4")
                .putThMetadata(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadThFileName.jpg")
                .putUserMetadata("aaa", "111")
                .putThUserMetadata("bbb", "222")
                .upload();
        Assert.notNull(fileInfo, "文件上传失败！");
        log.info("被移动的文件上传成功：{}", fileInfo);

        // 为了防止有些存储平台（例如又拍云）刚上传完后就进行操作会出现错误，这里等待一会
        ThreadUtil.sleep(1000);
        return fileInfo;
    }

    /**
     * 测试移动到不同路径下
     */
    @Test
    public void path() {
        FileInfo fileInfo = upload();
        log.info("测试移动到其它路径下：{}", fileInfo);
        FileInfo destFileInfo =
                fileStorageService.move(fileInfo).setPath("move/").move();
        log.info("测试移动到其它路径下完成：{}", destFileInfo);
    }

    /**
     * 测试移动到同路径下不同文件名
     */
    @Test
    public void filename() {
        FileInfo fileInfo = upload();
        log.info("测试移动到同路径下且带进度监听：{}", fileInfo);
        FileInfo destFileInfo = fileStorageService
                .move(fileInfo)
                .setFilename("aaaMove." + FileNameUtil.extName(fileInfo.getFilename()))
                .setThFilename("aaaMove.min." + FileNameUtil.extName(fileInfo.getThFilename()))
                .setProgressListener((progressSize, allSize) ->
                        log.info("文件移动进度：{} {}%", progressSize, progressSize * 100 / allSize))
                .move();
        log.info("测试移动到同路径下且带进度监听完成：{}", destFileInfo);
    }

    /**
     * 测试跨存储平台移动
     */
    @Test
    public void cross() {
        FileInfo fileInfo = upload();
        log.info("测试移动到其它存储平台下：{}", fileInfo);
        FileInfo destFileInfo = fileStorageService
                .move(fileInfo)
                .setPlatform("local-plus-1")
                .setProgressListener((progressSize, allSize) ->
                        log.info("文件移动进度：{} {}%", progressSize, progressSize * 100 / allSize))
                .move();
        log.info("测试移动到其它存储平台下完成：{}", destFileInfo);
    }
}
