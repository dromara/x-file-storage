package org.dromara.x.file.storage.core.upload;

import java.util.List;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-列举已上传的分片执行器
 */
public class ListPartsActuator {
    private final FileStorageService fileStorageService;
    private final ListPartsPretreatment pre;

    public ListPartsActuator(ListPartsPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行列举已上传的分片
     */
    public List<FilePartInfo> execute() {
        FileInfo fileInfo = pre.getFileInfo();
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        return fileStorage.listParts(pre);
    }
}
