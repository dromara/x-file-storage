package org.dromara.x.file.storage.core.upload;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;

/**
 * 手动分片上传-取消预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class AbortMultipartUploadPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;

    /**
     * 如果条件为 true 则：设置文件存储服务类
     * @param flag 条件
     * @param fileStorageService 文件存储服务类
     * @return 手动分片上传-取消预处理器
     */
    public AbortMultipartUploadPretreatment setFileStorageService(boolean flag, FileStorageService fileStorageService) {
        if (flag) setFileStorageService(fileStorageService);
        return this;
    }

    /**
     * 如果条件为 true 则：设置文件信息
     * @param flag 条件
     * @param fileInfo 文件信息
     * @return 手动分片上传-取消预处理器
     */
    public AbortMultipartUploadPretreatment setFileInfo(boolean flag, FileInfo fileInfo) {
        if (flag) setFileInfo(fileInfo);
        return this;
    }

    /**
     * 执行取消
     */
    public FileInfo abort() {
        return new AbortMultipartUploadActuator(this).execute();
    }
}
