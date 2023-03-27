package cn.xuyanwu.spring.file.storage;


import cn.hutool.core.lang.Dict;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FileInfo implements Serializable {

    /**
     * 文件id
     */
    private String id;

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 基础存储路径
     */
    private String basePath;

    /**
     * 存储路径
     */
    private String path;

    /**
     * 文件扩展名
     */
    private String ext;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 缩略图访问路径
     */
    private String thUrl;

    /**
     * 缩略图名称
     */
    private String thFilename;

    /**
     * 缩略图大小，单位字节
     */
    private Long thSize;

    /**
     * 缩略图 MIME 类型
     */
    private String thContentType;

    /**
     * 文件所属对象id
     */
    private String objectId;

    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    private String objectType;

    /**
     * 附加属性字典
     */
    private Dict attr;

    /**
     * 文件的访问控制列表，一般情况下只有对象存储支持该功能，支持 String 或对应存储平台的 ACL 对象
     * <pre class="code">
     * //方式一，通过字符串设置通用的 ACL 详情：{@link cn.xuyanwu.spring.file.storage.constant.Constant.ACL }
     * setFileAcl(ACL.PUBLIC_READ);
     * //方式二，针对指定存储平台设置更复杂的权限控制，以华为云 OBS 为例
     * AccessControlList acl = new AccessControlList();
     * Owner owner = new Owner();
     * owner.setId("ownerid");
     * acl.setOwner(owner);
     * // 保留Owner的完全控制权限（注：如果不设置该权限，该对象Owner自身将没有访问权限）
     * acl.grantPermission(new CanonicalGrantee("ownerid"), Permission.PERMISSION_FULL_CONTROL);
     * // 为指定用户设置完全控制权限
     * acl.grantPermission(new CanonicalGrantee("userid"), Permission.PERMISSION_FULL_CONTROL);
     * // 为所有用户设置读权限
     * acl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);
     * setFileAcl(acl);
     * <pre/>
     */
    private Object fileAcl;

    /**
     * 缩略图的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link cn.xuyanwu.spring.file.storage.FileInfo#setFileAcl}
     */
    private Object thFileAcl;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 分片上传ID
     */
    private String uploadId;

    private static final long serialVersionUID = 1L;
}
