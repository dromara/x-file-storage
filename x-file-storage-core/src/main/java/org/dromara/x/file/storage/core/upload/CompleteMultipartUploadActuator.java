package org.dromara.x.file.storage.core.upload;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-完成执行器
 */
public class CompleteMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final CompleteMultipartUploadPretreatment pre;

    public CompleteMultipartUploadActuator(CompleteMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行完成
     */
    public FileInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        fileStorage.completeMultipartUpload(pre);
        return fileInfo;
    }
}
