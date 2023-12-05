package org.dromara.x.file.storage.core.upload;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;

/**
 * 手动分片上传-列举已上传的分片预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class ListPartsPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;

    /**
     * 执行列举已上传的分片
     */
    public List<FilePartInfo> listParts() {
        return new ListPartsActuator(this).execute();
    }
}
