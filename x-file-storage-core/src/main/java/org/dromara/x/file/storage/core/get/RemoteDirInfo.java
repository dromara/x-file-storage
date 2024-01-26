package org.dromara.x.file.storage.core.get;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 远程目录信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class RemoteDirInfo {
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
     * 目录名称
     */
    private String name;
}
