package org.dromara.x.file.storage.core.get;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 文件信息列举结果
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ListFilesResult {
    /**
     * 目录列表
     */
    private List<RemoteDirInfo> dirList;
    /**
     * 文件列表
     */
    private List<RemoteFileInfo> fileList;
    /**
     * 存储平台名称
     */
    private String platform;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 路径，需要与上传时传入的路径保持一致
     */
    private String path = "";
    /**
     * 文件名前缀
     */
    private String filenamePrefix = "";
    /**
     * 本次列举的最大文件数量
     */
    private Integer maxFiles;
    /**
     * 列表是否被截断，就是当前目录下还有其它文件超出最大文件数量未被列举
     */
    private Boolean isTruncated;
    /**
     * 本次列举的起始位置
     */
    private String marker;
    /**
     * 下次列举的起始位置
     */
    private String nextMarker;
}
