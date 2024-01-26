package org.dromara.x.file.storage.test;

import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.get.ListFilesSupportInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FileStorageServiceGetTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试列举文件
     */
    @Test
    public void listFiles() {
        ListFilesSupportInfo isSupportListFiles = fileStorageService.isSupportListFiles();
        if (!isSupportListFiles.getIsSupport()) {
            log.info("暂不支持列举文件");
            return;
        }

        ListFilesResult result = fileStorageService.listFiles().setPath("test/").listFiles();

        result.getDirList().forEach(info -> log.info("目录：{}", info));
        log.info("列举目录完成，共 {} 个目录", result.getDirList().size());
        result.getFileList().forEach(info -> log.info("文件：{}", info));
        log.info("列举文件完成，共 {} 个文件", result.getFileList().size());
        log.info("列举文件全部结果：{}", result);
    }
}
