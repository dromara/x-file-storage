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
     * 执行取消
     */
    public FileInfo abort() {
        return new AbortMultipartUploadActuator(this).execute();
    }
}
