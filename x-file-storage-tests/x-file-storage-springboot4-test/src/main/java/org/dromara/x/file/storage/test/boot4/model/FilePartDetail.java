package org.dromara.x.file.storage.test.boot4.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件分片信息表，仅在手动分片上传时使用
 */
@Data
@TableName(value = "file_part_detail")
public class FilePartDetail {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField(value = "platform")
    private String platform;

    @TableField(value = "upload_id")
    private String uploadId;

    @TableField(value = "e_tag")
    private String eTag;

    @TableField(value = "part_number")
    private Integer partNumber;

    @TableField(value = "part_size")
    private Long partSize;

    @TableField(value = "hash_info")
    private String hashInfo;

    @TableField(value = "create_time")
    private Date createTime;

    public static final String COL_UPLOAD_ID = "upload_id";
}
