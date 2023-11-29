package org.dromara.x.file.storage.core.upload;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-取消执行器
 */
public class AbortMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final AbortMultipartUploadPretreatment pre;

    public AbortMultipartUploadActuator(AbortMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行取消
     */
    public FileInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        fileStorage.abortMultipartUpload(pre);
        return fileInfo;
    }
}
