package org.dromara.x.file.storage.core.get;

import java.util.Date;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 文件信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FileFileInfo {
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * MIME 类型
     */
    private String contentType;
    /**
     * MD5
     */
    private String contentMd5;
    /**
     * 最后修改时间
     */
    private Date lastModified;
    /**
     * 文件元数据
     */
    private Map<String, Object> metadata;
    /**
     * 文件用户元数据
     */
    private Map<String, Object> userMetadata;
    /**
     * 原始数据
     */
    private Object original;
}
