package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-初始化执行器
 */
public class InitiateMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final InitiateMultipartUploadPretreatment pre;

    public InitiateMultipartUploadActuator(InitiateMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行初始化
     */
    public FileInfo execute() {
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(pre.getPlatform());
        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());

        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setPath(pre.getPath());

        if (StrUtil.isNotBlank(pre.getSaveFilename())) {
            fileInfo.setFilename(pre.getSaveFilename());
        } else {
            fileInfo.setFilename(
                    IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }

        fileStorage.initiateMultipartUpload(fileInfo, pre);
        return fileInfo;
    }
}
