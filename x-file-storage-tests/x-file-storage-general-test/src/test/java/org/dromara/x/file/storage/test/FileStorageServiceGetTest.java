package org.dromara.x.file.storage.test;

import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.get.FileDirInfo;
import org.dromara.x.file.storage.core.get.FileFileInfo;
import org.dromara.x.file.storage.core.get.FileFileInfoList;
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

        FileFileInfoList list = fileStorageService.listFiles().setPath("test/").listFiles();
        for (FileDirInfo info : list.getDirList()) {
            log.info("列举目录：{}", info);
        }
        log.info("列举目录完成，共 {} 个目录", list.getDirList().size());

        for (FileFileInfo info : list.getFileList()) {
            log.info("列举文件：{}", info);
        }
        log.info("列举文件完成，共 {} 个文件", list.getFileList().size());
        log.info("列举文件结果：{}", list);
    }
}
