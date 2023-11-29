package org.dromara.x.file.storage.core.upload;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-上传分片执行器
 */
public class UploadPartActuator {
    private final FileStorageService fileStorageService;
    private final UploadPartPretreatment pre;

    public UploadPartActuator(UploadPartPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行上传
     */
    public FilePartInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();

        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo);

        return fileStorage.uploadPart(pre);
    }
}
