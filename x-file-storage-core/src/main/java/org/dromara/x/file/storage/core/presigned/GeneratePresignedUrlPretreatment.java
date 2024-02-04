package org.dromara.x.file.storage.core.presigned;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 生成预签名 URL 预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class GeneratePresignedUrlPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 存储平台名称
     */
    private String platform;
    /**
     * 路径
     */
    private String path = "";
    /**
     * 文件名
     */
    private String filename = "";
    /**
     * 生效开始时间，仅 Azure Blob Storage 存储平台支持
     */
    private Date startTime;
    /**
     * 到期时间
     */
    private Date expiration;
    /**
     * HTTP 请求方法，可以直接传入字符串或对应存储平台支持的类型
     * {@link org.dromara.x.file.storage.core.constant.Constant.GeneratePresignedUrl.Method}
     */
    private Object method;
    /**
     * 特殊操作符，可以直接传入字符串或对应存储平台支持的类型，仅华为云存储平台支持
     */
    private Object specialParam;
    /**
     * 请求头（元数据）
     */
    private Map<String, String> headers;
    /**
     * 用户元数据
     */
    private Map<String, String> userMetadata;
    /**
     * 查询参数
     */
    private Map<String, String> queryParams;
    /**
     * 要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    private Map<String, String> responseHeaders;

    /**
     * 如果条件为 true 则：设置文件存储服务类
     * @param flag 条件
     * @param fileStorageService 文件存储服务类
     * @return 获取文件预处理器
     */
    public GeneratePresignedUrlPretreatment setFileStorageService(boolean flag, FileStorageService fileStorageService) {
        if (flag) setFileStorageService(fileStorageService);
        return this;
    }

    /**
     * 设置存储平台名称（如果条件为 true）
     * @param flag 条件
     * @param platform 存储平台名称
     * @return 获取文件预处理器
     */
    public GeneratePresignedUrlPretreatment setPlatform(boolean flag, String platform) {
        if (flag) setPlatform(platform);
        return this;
    }

    /**
     * 设置路径，需要与上传时传入的路径保持一致（如果条件为 true）
     * @param flag 条件
     * @param path 文件访问地址
     * @return 获取文件预处理器
     */
    public GeneratePresignedUrlPretreatment setPath(boolean flag, String path) {
        if (flag) setPath(path);
        return this;
    }

    /**
     * 设置到期时间（如果条件为 true）
     * @param flag 条件
     * @param expiration 到期时间
     * @return 获取文件预处理器
     */
    public GeneratePresignedUrlPretreatment setExpiration(boolean flag, Date expiration) {
        if (flag) setExpiration(expiration);
        return this;
    }

    /**
     * 获取请求头（元数据）
     */
    public Map<String, String> getHeaders() {
        if (headers == null) headers = new LinkedHashMap<>();
        return headers;
    }

    /**
     * 设置请求头（元数据）
     */
    public GeneratePresignedUrlPretreatment putHeaders(boolean flag, String key, String value) {
        if (flag) putHeaders(key, value);
        return this;
    }

    /**
     * 设置请求头（元数据）
     */
    public GeneratePresignedUrlPretreatment putHeaders(String key, String value) {
        getHeaders().put(key, value);
        return this;
    }

    /**
     * 设置请求头（元数据）
     */
    public GeneratePresignedUrlPretreatment putHeadersAll(boolean flag, Map<String, String> headers) {
        if (flag) putHeadersAll(headers);
        return this;
    }

    /**
     * 设置请求头（元数据）
     */
    public GeneratePresignedUrlPretreatment putHeadersAll(Map<String, String> headers) {
        getHeaders().putAll(headers);
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
    public GeneratePresignedUrlPretreatment putUserMetadata(boolean flag, String key, String value) {
        if (flag) putUserMetadata(key, value);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public GeneratePresignedUrlPretreatment putUserMetadata(String key, String value) {
        getUserMetadata().put(key, value);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public GeneratePresignedUrlPretreatment putUserMetadataAll(boolean flag, Map<String, String> userMetadata) {
        if (flag) putUserMetadataAll(userMetadata);
        return this;
    }

    /**
     * 设置文件用户元数据
     */
    public GeneratePresignedUrlPretreatment putUserMetadataAll(Map<String, String> userMetadata) {
        getUserMetadata().putAll(userMetadata);
        return this;
    }

    /**
     * 获取查询参数
     */
    public Map<String, String> getQueryParams() {
        if (queryParams == null) queryParams = new LinkedHashMap<>();
        return queryParams;
    }

    /**
     * 设置查询参数
     */
    public GeneratePresignedUrlPretreatment putQueryParams(boolean flag, String key, String value) {
        if (flag) putQueryParams(key, value);
        return this;
    }

    /**
     * 设置查询参数
     */
    public GeneratePresignedUrlPretreatment putQueryParams(String key, String value) {
        getQueryParams().put(key, value);
        return this;
    }

    /**
     * 设置查询参数
     */
    public GeneratePresignedUrlPretreatment putQueryParamsAll(boolean flag, Map<String, String> queryParams) {
        if (flag) putQueryParamsAll(queryParams);
        return this;
    }

    /**
     * 设置查询参数
     */
    public GeneratePresignedUrlPretreatment putQueryParamsAll(Map<String, String> queryParams) {
        getQueryParams().putAll(queryParams);
        return this;
    }

    /**
     * 设置要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    public Map<String, String> getResponseHeaders() {
        if (responseHeaders == null) responseHeaders = new LinkedHashMap<>();
        return responseHeaders;
    }

    /**
     * 设置要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    public GeneratePresignedUrlPretreatment putResponseHeaders(boolean flag, String key, String value) {
        if (flag) putResponseHeaders(key, value);
        return this;
    }

    /**
     * 设置要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    public GeneratePresignedUrlPretreatment putResponseHeaders(String key, String value) {
        getResponseHeaders().put(key, value);
        return this;
    }

    /**
     * 设置要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    public GeneratePresignedUrlPretreatment putResponseHeadersAll(boolean flag, Map<String, String> responseHeader) {
        if (flag) putResponseHeadersAll(responseHeader);
        return this;
    }

    /**
     * 设置要在服务响应中重写的标头，实际效果以每个存储平台支持为准
     */
    public GeneratePresignedUrlPretreatment putResponseHeadersAll(Map<String, String> responseHeader) {
        getResponseHeaders().putAll(responseHeader);
        return this;
    }

    /**
     * 设置文件版本 ID
     */
    public GeneratePresignedUrlPretreatment setVersionId(String versionId) {
        return putQueryParams("versionId", versionId);
    }

    /**
     * 设置文件版本 ID
     */
    public GeneratePresignedUrlPretreatment setVersionId(boolean flag, String versionId) {
        if (flag) putQueryParams("versionId", versionId);
        return this;
    }

    /**
     * 执行生成预签名 URL
     */
    public GeneratePresignedUrlResult generatePresignedUrl() {
        return new GeneratePresignedUrlActuator(this).execute();
    }

    /**
     * 执行生成预签名 URL，此方法仅限内部使用
     */
    public GeneratePresignedUrlResult generatePresignedUrl(
            FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        return new GeneratePresignedUrlActuator(this).execute(fileStorage, aspectList);
    }
}
