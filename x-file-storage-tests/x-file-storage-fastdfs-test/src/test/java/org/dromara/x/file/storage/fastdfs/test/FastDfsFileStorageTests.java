package org.dromara.x.file.storage.fastdfs.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.annotation.Resource;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/23 10:35
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class FastDfsFileStorageTests {

    /**
     * File name
     */
    private static final String FILE_NAME = "M00/00/00/rByFDmVu22GAGIUXAAAANH8O2pA060.txt";

    @Resource
    private FileStorageService fileStorageService;

    @Test
    void upload() {
        File file = FileUtil.file("fastdfs.txt");

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            MultipartFile multipartFile =
                    new MockMultipartFile("uploadFile", file.getName(), "text/plain", fileInputStream);
            FileInfo upload = fileStorageService.of(multipartFile).upload();
            Console.log(upload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void download() {
        File tempFile = FileUtil.createTempFile();
        Console.log(tempFile);
        fileStorageService
                .download(new FileInfo().setPlatform("fastdfs-1").setFilename(FILE_NAME))
                .file(tempFile);
    }

    @Test
    void exists() {
        boolean exists = fileStorageService.exists(
                new FileInfo().setPlatform("fastdfs-1").setFilename(FILE_NAME));
        Console.log("exists: " + exists);
    }

    @Test
    void delete() {
        boolean deleted = fileStorageService.delete(
                new FileInfo().setPlatform("fastdfs-1").setFilename(FILE_NAME));
        Console.log("deleted: " + deleted);
    }
}
