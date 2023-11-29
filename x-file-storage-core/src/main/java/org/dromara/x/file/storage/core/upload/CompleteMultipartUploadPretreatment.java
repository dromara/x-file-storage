package org.dromara.x.file.storage.core.upload;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;

/**
 * 手动分片上传-完成预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class CompleteMultipartUploadPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;
    /**
     * 文件分片信息，不传则自动使用全部已上传的分片
     */
    private List<FilePartInfo> partInfoList;

    /**
     * 执行完成
     */
    public FileInfo complete() {
        return new CompleteMultipartUploadActuator(this).execute();
    }
}
