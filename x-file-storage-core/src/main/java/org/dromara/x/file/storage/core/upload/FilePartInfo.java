package org.dromara.x.file.storage.core.upload;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;

/**
 * 文件分片信息
 */
@Data
@NoArgsConstructor
public class FilePartInfo {
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 上传ID
     */
    private String uploadId;
    /**
     * 分片 ETag
     */
    private String eTag;
    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    private Integer partNumber;
    /**
     * 分片大小
     */
    private Long partSize;
    /**
     * 分片最后修改时间，仅在获取分片信息时使用
     */
    private Date lastModified;

    public FilePartInfo(FileInfo fileInfo) {
        platform = fileInfo.getPlatform();
        uploadId = fileInfo.getUploadId();
    }
}
