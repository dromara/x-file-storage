package org.dromara.x.file.storage.fastdfs.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/23 10:35
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class FastDfsTests {
    
    @Resource
    private FileStorageService fileStorageService;
    
    @Test
    void upload() {
        File file = FileUtil.file("test.txt");
        
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            MultipartFile multipartFile = new MockMultipartFile("uploadFile", file.getName(), "text/plain", fileInputStream);
            FileInfo upload = fileStorageService.of(multipartFile).upload();
            Console.log(upload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}