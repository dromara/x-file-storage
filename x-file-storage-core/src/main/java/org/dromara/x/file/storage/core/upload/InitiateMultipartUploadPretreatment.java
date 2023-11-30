package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.lang.Dict;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;

/**
 * 手动分片上传-初始化预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class InitiateMultipartUploadPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 要上传到的平台
     */
    private String platform;
    /**
     * 文件所属对象id
     */
    private String objectId;
    /**
     * 文件所属对象类型
     */
    private String objectType;
    /**
     * 文件存储路径
     */
    private String path = "";
    /**
     * 原始文件名
     */
    private String originalFilename;
    /**
     * 保存文件名，如果不设置则自动生成
     */
    private String saveFilename;
    /**
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件的 MIME 类型
     */
    String contentType;
    /**
     * 文件元数据
     */
    private Map<String, String> metadata;
    /**
     * 文件用户元数据
     */
    private Map<String, String> userMetadata;

    /**
     * 不支持元数据时抛出异常
     */
    private Boolean notSupportMetadataThrowException;
    /**
     * 不支持 ACL 时抛出异常
     */
    private Boolean notSupportAclThrowException;
    /**
     * 附加属性字典
     */
    private Dict attr;
    /**
     * 文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    private Object fileAcl;

    /**
     * 设置要上传到的平台
     */
    public InitiateMultipartUploadPretreatment setPlatform(boolean flag, String platform) {
        if (flag) setPlatform(platform);
        return this;
    }

    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    public InitiateMultipartUploadPretreatment setObjectId(boolean flag, Object objectId) {
        if (flag) setObjectId(objectId);
        return this;
    }

    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    public InitiateMultipartUploadPretreatment setObjectId(Object objectId) {
        this.objectId = objectId == null ? null : objectId.toString();
        return this;
    }

    /**
     * 设置文件所属对象类型
     */
    public InitiateMultipartUploadPretreatment setObjectType(boolean flag, String objectType) {
        if (flag) setObjectType(objectType);
        return this;
    }

    /**
     * 设置文文件存储路径
     */
    public InitiateMultipartUploadPretreatment setPath(boolean flag, String path) {
        if (flag) setPath(path);
        return this;
    }

    /**
     * 设置原始文件名
     */
    public InitiateMultipartUploadPretreatment setOriginalFilename(boolean flag, String originalFilename) {
        if (flag) setOriginalFilename(originalFilename);
        return this;
    }

    /**
     * 设置保存文件名，如果不设置则自动生成
     */
    public InitiateMultipartUploadPretreatment setSaveFilename(boolean flag, String saveFilename) {
        if (flag) setSaveFilename(saveFilename);
        return this;
    }

    /**
     * 设置文件大小，单位字节
     */
    public InitiateMultipartUploadPretreatment setSize(boolean flag, Long size) {
        if (flag) setSize(size);
        return this;
    }

    /**
     * 文件的 MIME 类型
     */
    public InitiateMultipartUploadPretreatment setSize(boolean flag, String contentType) {
        if (flag) setContentType(contentType);
        return this;
    }

    /**
     * 获取文件元数据
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) metadata = new LinkedHashMap<>();
        return metadata;
    }

    /**
     * 设置文件元数据
     */
    public InitiateMultipartUploadPretreatment putMetadata(boolean flag, String key, String value) {
        if (flag) putMetadata(key, value);
        return this;
    }

    /**
     * 设置文件元数据
     */
    public InitiateMultipartUploadPretreatment putMetadata(String key, String value) {
        getMetadata().put(key, value);
        return this;
    }

    /**
     * 设置文件元数据
     */
    public InitiateMultipartUploadPretreatment putMetadataAll(boolean flag, Map<String, String> metadata) {
        if (flag) putMetadataAll(metadata);
        return this;
    }

    /**
     * 设置文件元数据
     */
    public InitiateMultipartUploadPretreatment putMetadataAll(Map<String, String> metadata) {
        getMetadata().putAll(metadata);
        return this;
    }

    /**
     * 获取文件用户元数据
     */
    public Map<String, String> getUserMetadata() {
        if (userMetadata == null) userMetadata = new LinkedHashMap<>();
        return userMetadata;
    }

    /**
     * 设置文件用户元数据
     */
    public InitiateMultipartUploadPretreatment putUserMetadata(boolean flag, String key, String value) {
        if (flag) putUserMetadata(key, value);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public InitiateMultipartUploadPretreatment putUserMetadata(String key, String value) {
        getUserMetadata().put(key, value);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public InitiateMultipartUploadPretreatment putUserMetadataAll(boolean flag, Map<String, String> metadata) {
        if (flag) putUserMetadataAll(metadata);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public InitiateMultipartUploadPretreatment putUserMetadataAll(Map<String, String> metadata) {
        getUserMetadata().putAll(metadata);
        return this;
    }

    /**
     * 设置不支持元数据时抛出异常
     */
    public InitiateMultipartUploadPretreatment setNotSupportMetadataThrowException(
            boolean flag, Boolean notSupportMetadataThrowException) {
        if (flag) this.notSupportMetadataThrowException = notSupportMetadataThrowException;
        return this;
    }

    /**
     * 设置不支持 ACL 时抛出异常
     */
    public InitiateMultipartUploadPretreatment setNotSupportAclThrowException(
            boolean flag, Boolean notSupportAclThrowException) {
        if (flag) this.notSupportAclThrowException = notSupportAclThrowException;
        return this;
    }

    /**
     * 获取附加属性字典
     */
    public Dict getAttr() {
        if (attr == null) attr = new Dict();
        return attr;
    }

    /**
     * 设置附加属性
     */
    public InitiateMultipartUploadPretreatment putAttr(boolean flag, String key, Object value) {
        if (flag) putAttr(key, value);
        return this;
    }

    /**
     * 设置附加属性
     */
    public InitiateMultipartUploadPretreatment putAttr(String key, Object value) {
        getAttr().put(key, value);
        return this;
    }

    /**
     * 设置附加属性
     */
    public InitiateMultipartUploadPretreatment putAttrAll(boolean flag, Map<String, Object> attr) {
        if (flag) putAttrAll(attr);
        return this;
    }

    /**
     * 设置附加属性
     */
    public InitiateMultipartUploadPretreatment putAttrAll(Map<String, Object> attr) {
        getAttr().putAll(attr);
        return this;
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    public InitiateMultipartUploadPretreatment setFileAcl(boolean flag, Object acl) {
        if (flag) setFileAcl(acl);
        return this;
    }

    /**
     * 执行初始化
     */
    public FileInfo init() {
        return new InitiateMultipartUploadActuator(this).execute();
    }
}
