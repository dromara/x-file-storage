package cn.xuyanwu.spring.file.storage.spring;

import cn.xuyanwu.spring.file.storage.FileStorageProperties.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
     * 本地存储
     */
    private List<SpringLocalConfig> local = new ArrayList<>();
    /**
     * 本地存储
     */
    private List<SpringLocalPlusConfigConfig> localPlus = new ArrayList<>();
    /**
     * 华为云 OBS
     */
    private List<SpringHuaweiObsConfigConfig> huaweiObs = new ArrayList<>();
    /**
     * 阿里云 OSS
     */
    private List<SpringAliyunOssConfig> aliyunOss = new ArrayList<>();
    /**
     * 七牛云 Kodo
     */
    private List<SpringQiniuKodoConfig> qiniuKodo = new ArrayList<>();
    /**
     * 腾讯云 COS
     */
    private List<SpringTencentCosConfig> tencentCos = new ArrayList<>();
    /**
     * 百度云 BOS
     */
    private List<SpringBaiduBosConfig> baiduBos = new ArrayList<>();
    /**
     * 又拍云 USS
     */
    private List<SpringUpyunUssConfig> upyunUss = new ArrayList<>();
    /**
     * MinIO USS
     */
    private List<SpringMinioConfig> minio = new ArrayList<>();

    /**
     * Amazon S3
     */
    private List<SpringAmazonS3Config> amazonS3 = new ArrayList<>();

    /**
     * FTP
     */
    private List<SpringFtpConfig> ftp = new ArrayList<>();

    /**
     * FTP
     */
    private List<SpringSftpConfig> sftp = new ArrayList<>();

    /**
     * WebDAV
     */
    private List<SpringWebDavConfig> WebDav = new ArrayList<>();

    /**
     * Google Cloud Storage
     */
    private List<SpringGoogleCloudStorageConfig> googleCloudStorage = new ArrayList<>();

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
    public static class SpringLocalPlusConfigConfig extends LocalPlusConfig {
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
    public static class SpringHuaweiObsConfigConfig extends HuaweiObsConfig {
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
     * Google Cloud Storage
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
