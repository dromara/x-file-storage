package org.dromara.x.file.storage.core.upload;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;

/**
 * 文件分片信息列出结果
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FilePartInfoList {
    /**
     * 文件信息
     */
    private FileInfo fileInfo;
    /**
     * 分片列表
     */
    private List<FilePartInfo> list;
    /**
     * 本次列出的最大分片数量
     */
    private Integer maxParts;
    /**
     * 列表是否被截断，就是当前 uploadId下还有其它分片超出最大分片数量未被列出
     */
    private Boolean isTruncated;
    /**
     * 本次列举的起始位置
     */
    private Integer partNumberMarker;
    /**
     * 下次列举的起始位置
     */
    private Integer nextPartNumberMarker;
}
