package org.dromara.x.file.storage.core.get;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文件信息列出结果
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FileFileInfoList {
    /**
     * 目录列表
     */
    private List<FileDirInfo> dirList;
    /**
     * 文件列表
     */
    private List<FileFileInfo> fileList;
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
     * 本次列出的最大文件数量
     */
    private Integer maxFiles;
    /**
     * 列表是否被截断，就是当前 uploadId下还有其它分片超出最大分片数量未被列出
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
