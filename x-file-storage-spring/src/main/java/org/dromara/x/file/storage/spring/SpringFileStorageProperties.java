package org.dromara.x.file.storage.spring;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageProperties.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
@ConditionalOnMissingBean(SpringFileStorageProperties.class)
@ConfigurationProperties(prefix = "spring.file-storage")
public class SpringFileStorageProperties {

    /**
     * 默认存储平台
     */
    private String defaultPlatform = "local";
    /**
     * 缩略图后缀，例如【.min.jpg】【.png】
     */
    private String thumbnailSuffix = ".min.jpg";
    /**
     * 上传时不支持元数据时抛出异常
     */
    private Boolean uploadNotSupportMetadataThrowException = true;
    /**
     * 启用 byte[] 文件包装适配器
     */
    private Boolean enableByteFileWrapper = true;
    /**
     * 启用 URI 文件包装适配器，包含 URL 和 String
     */
    private Boolean enableUriFileWrapper = true;
    /**
     * 启用 InputStream 文件包装适配器
     */
    private Boolean enableInputStreamFileWrapper = true;
    /**
     * 启用本地文件包装适配器
     */
    private Boolean enableLocalFileWrapper = true;
    /**
     * 启用 HttpServletRequest 文件包装适配器
     */
    private Boolean enableHttpServletRequestFileWrapper = true;
    /**
     * 启用 Multipart 文件包装适配器
     */
    private Boolean enableMultipartFileWrapper = true;
    /**
     * 本地存储
     */
    private List<? extends SpringLocalConfig> local = new ArrayList<>();
    /**
     * 本地存储
     */
    private List<? extends SpringLocalPlusConfig> localPlus = new ArrayList<>();
    /**
     * 华为云 OBS
     */
    private List<? extends SpringHuaweiObsConfig> huaweiObs = new ArrayList<>();
    /**
     * 阿里云 OSS
     */
    private List<? extends SpringAliyunOssConfig> aliyunOss = new ArrayList<>();
    /**
     * 七牛云 Kodo
     */
    private List<? extends SpringQiniuKodoConfig> qiniuKodo = new ArrayList<>();
    /**
     * 腾讯云 COS
     */
    private List<? extends SpringTencentCosConfig> tencentCos = new ArrayList<>();
    /**
     * 百度云 BOS
     */
    private List<? extends SpringBaiduBosConfig> baiduBos = new ArrayList<>();
    /**
     * 又拍云 USS
     */
    private List<? extends SpringUpyunUssConfig> upyunUss = new ArrayList<>();
    /**
     * MinIO USS
     */
    private List<? extends SpringMinioConfig> minio = new ArrayList<>();

    /**
     * Amazon S3
     */
    private List<? extends SpringAmazonS3Config> amazonS3 = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SpringFtpConfig> ftp = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SpringSftpConfig> sftp = new ArrayList<>();

    /**
     * WebDAV
     */
    private List<? extends SpringWebDavConfig> WebDav = new ArrayList<>();

    /**
     * GoogleCloud Storage
     */
    private List<? extends SpringGoogleCloudStorageConfig> googleCloudStorage = new ArrayList<>();


    /**
     * 转换成 FileStorageProperties ，并过滤掉没有启用的存储平台
     */
    public FileStorageProperties toFileStorageProperties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDefaultPlatform(defaultPlatform);
        properties.setThumbnailSuffix(thumbnailSuffix);
        properties.setUploadNotSupportMetadataThrowException(uploadNotSupportMetadataThrowException);
        properties.setLocal(local.stream().filter(SpringLocalConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setLocalPlus(localPlus.stream().filter(SpringLocalPlusConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setHuaweiObs(huaweiObs.stream().filter(SpringHuaweiObsConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setAliyunOss(aliyunOss.stream().filter(SpringAliyunOssConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setQiniuKodo(qiniuKodo.stream().filter(SpringQiniuKodoConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setTencentCos(tencentCos.stream().filter(SpringTencentCosConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setBaiduBos(baiduBos.stream().filter(SpringBaiduBosConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setUpyunUss(upyunUss.stream().filter(SpringUpyunUssConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setMinio(minio.stream().filter(SpringMinioConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setAmazonS3(amazonS3.stream().filter(SpringAmazonS3Config::getEnableStorage).collect(Collectors.toList()));
        properties.setFtp(ftp.stream().filter(SpringFtpConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setSftp(sftp.stream().filter(SpringSftpConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setWebDav(WebDav.stream().filter(SpringWebDavConfig::getEnableStorage).collect(Collectors.toList()));
        properties.setGoogleCloudStorage(googleCloudStorage.stream().filter(SpringGoogleCloudStorageConfig::getEnableStorage).collect(Collectors.toList()));
        return properties;
    }

    /**
     * 本地存储
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringLocalConfig extends LocalConfig {
        /**
         * 本地存储访问路径
         */
        private String[] pathPatterns = new String[0];
        /**
         * 启用本地存储
         */
        private Boolean enableStorage = false;
        /**
         * 启用本地访问
         */
        private Boolean enableAccess = false;
    }

    /**
     * 本地存储升级版
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringLocalPlusConfig extends LocalPlusConfig {
        /**
         * 本地存储访问路径
         */
        private String[] pathPatterns = new String[0];
        /**
         * 启用本地存储
         */
        private Boolean enableStorage = false;
        /**
         * 启用本地访问
         */
        private Boolean enableAccess = false;
    }

    /**
     * 华为云 OBS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringHuaweiObsConfig extends HuaweiObsConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 阿里云 OSS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringAliyunOssConfig extends AliyunOssConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 七牛云 Kodo
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringQiniuKodoConfig extends QiniuKodoConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 腾讯云 COS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringTencentCosConfig extends TencentCosConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 百度云 BOS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringBaiduBosConfig extends BaiduBosConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * 又拍云 USS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringUpyunUssConfig extends UpyunUssConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * MinIO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringMinioConfig extends MinioConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * Amazon S3
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringAmazonS3Config extends AmazonS3Config {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * FTP
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringFtpConfig extends FtpConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * SFTP
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringSftpConfig extends SftpConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * WebDAV
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringWebDavConfig extends WebDavConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

    /**
     * GoogleCloud Storage
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpringGoogleCloudStorageConfig extends GoogleCloudStorageConfig {
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
    }

}
