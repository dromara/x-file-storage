package org.dromara.x.file.storage.test.boot4.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件记录表
 */
@Data
@TableName(value = "file_detail")
public class FileDetail {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField(value = "url")
    private String url;

    @TableField(value = "`size`")
    private Long size;

    @TableField(value = "filename")
    private String filename;

    @TableField(value = "original_filename")
    private String originalFilename;

    @TableField(value = "base_path")
    private String basePath;

    @TableField(value = "`path`")
    private String path;

    @TableField(value = "ext")
    private String ext;

    @TableField(value = "content_type")
    private String contentType;

    @TableField(value = "platform")
    private String platform;

    @TableField(value = "th_url")
    private String thUrl;

    @TableField(value = "th_filename")
    private String thFilename;

    @TableField(value = "th_size")
    private Long thSize;

    @TableField(value = "th_content_type")
    private String thContentType;

    @TableField(value = "object_id")
    private String objectId;

    @TableField(value = "object_type")
    private String objectType;

    @TableField(value = "metadata")
    private String metadata;

    @TableField(value = "user_metadata")
    private String userMetadata;

    @TableField(value = "th_metadata")
    private String thMetadata;

    @TableField(value = "th_user_metadata")
    private String thUserMetadata;

    @TableField(value = "attr")
    private String attr;

    @TableField(value = "hash_info")
    private String hashInfo;

    @TableField(value = "upload_id")
    private String uploadId;

    @TableField(value = "upload_status")
    private Integer uploadStatus;

    @TableField(value = "create_time")
    private Date createTime;

    public static final String COL_ID = "id";
    public static final String COL_URL = "url";
}
