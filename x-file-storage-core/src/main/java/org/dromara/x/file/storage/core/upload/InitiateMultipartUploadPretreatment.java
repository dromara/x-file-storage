package org.dromara.x.file.storage.core.upload;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;

/**
 * 手动分片上传-初始化预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class InitiateMultipartUploadPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 要上传到的平台
     */
    private String platform;
    /**
     * 文件存储路径
     */
    private String path = "";

    /**
     * 保存文件名，如果不设置则自动生成
     */
    private String saveFilename;

    /**
     * 执行初始化
     */
    public FileInfo init() {
        return new InitiateMultipartUploadActuator(this).execute();
    }
}
