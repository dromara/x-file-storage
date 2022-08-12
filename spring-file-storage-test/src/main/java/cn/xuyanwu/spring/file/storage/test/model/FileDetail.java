package cn.xuyanwu.spring.file.storage.test.model;

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
    /**
     * 文件id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文件访问地址
     */
    @TableField(value = "url")
    private String url;

    /**
     * 文件大小，单位字节
     */
    @TableField(value = "`size`")
    private Long size;

    /**
     * 文件名称
     */
    @TableField(value = "filename")
    private String filename;

    /**
     * 原始文件名
     */
    @TableField(value = "original_filename")
    private String originalFilename;

    /**
     * 基础存储路径
     */
    @TableField(value = "base_path")
    private String basePath;

    /**
     * 存储路径
     */
    @TableField(value = "`path`")
    private String path;

    /**
     * 文件扩展名
     */
    @TableField(value = "ext")
    private String ext;

    /**
     * MIME类型
     */
    @TableField(value = "content_type")
    private String contentType;

    /**
     * 存储平台
     */
    @TableField(value = "platform")
    private String platform;

    /**
     * 缩略图访问路径
     */
    @TableField(value = "th_url")
    private String thUrl;

    /**
     * 缩略图名称
     */
    @TableField(value = "th_filename")
    private String thFilename;

    /**
     * 缩略图大小，单位字节
     */
    @TableField(value = "th_size")
    private Long thSize;

    /**
     * 缩略图MIME类型
     */
    @TableField(value = "th_content_type")
    private String thContentType;

    /**
     * 文件所属对象id
     */
    @TableField(value = "object_id")
    private String objectId;

    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    @TableField(value = "object_type")
    private String objectType;

    /**
     * 附加属性
     */
    @TableField(value = "attr")
    private String attr;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    public static final String COL_ID = "id";

    public static final String COL_URL = "url";

    public static final String COL_SIZE = "size";

    public static final String COL_FILENAME = "filename";

    public static final String COL_ORIGINAL_FILENAME = "original_filename";

    public static final String COL_BASE_PATH = "base_path";

    public static final String COL_PATH = "path";

    public static final String COL_EXT = "ext";

    public static final String COL_CONTENT_TYPE = "content_type";

    public static final String COL_PLATFORM = "platform";

    public static final String COL_TH_URL = "th_url";

    public static final String COL_TH_FILENAME = "th_filename";

    public static final String COL_TH_SIZE = "th_size";

    public static final String COL_TH_CONTENT_TYPE = "th_content_type";

    public static final String COL_OBJECT_ID = "object_id";

    public static final String COL_OBJECT_TYPE = "object_type";

    public static final String COL_ATTR = "attr";

    public static final String COL_CREATE_TIME = "create_time";
}
