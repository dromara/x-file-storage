package cn.xuyanwu.spring.file.storage.test.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.aspect.DeleteAspectChain;
import cn.xuyanwu.spring.file.storage.aspect.FileStorageAspect;
import cn.xuyanwu.spring.file.storage.aspect.UploadAspectChain;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 使用切面打印文件上传和删除的日志
 */
@Slf4j
@Component
public class LogFileStorageAspect implements FileStorageAspect {

    /**
     * 上传，成功返回文件信息，失败返回 null
     */
    @Override
    public FileInfo uploadAround(UploadAspectChain chain,FileInfo fileInfo,UploadPretreatment pre,FileStorage fileStorage,FileRecorder fileRecorder) {
        log.info("上传文件 before -> {}",fileInfo);
        fileInfo = chain.next(fileInfo,pre,fileStorage,fileRecorder);
        log.info("上传文件 after -> {}",fileInfo);
        return fileInfo;
    }

    /**
     * 删除文件，成功返回 true
     */
    @Override
    public boolean deleteAround(DeleteAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,FileRecorder fileRecorder) {
        log.info("删除文件 before -> {}",fileInfo);
        boolean res = chain.next(fileInfo,fileStorage,fileRecorder);
        log.info("删除文件 after -> {}",res);
        return res;
    }
}
