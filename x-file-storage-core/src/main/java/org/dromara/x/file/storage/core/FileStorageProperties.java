package org.dromara.x.file.storage.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.dromara.x.file.storage.core.constant.Constant;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FileStorageProperties {

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
    private List<? extends LocalConfig> local = new ArrayList<>();
    /**
     * 本地存储
     */
    private List<? extends LocalPlusConfig> localPlus = new ArrayList<>();
    /**
     * 华为云 OBS
     */
    private List<? extends HuaweiObsConfig> huaweiObs = new ArrayList<>();
    /**
     * 阿里云 OSS
     */
    private List<? extends AliyunOssConfig> aliyunOss = new ArrayList<>();
    /**
     * 七牛云 Kodo
     */
    private List<? extends QiniuKodoConfig> qiniuKodo = new ArrayList<>();
    /**
     * 腾讯云 COS
     */
    private List<? extends TencentCosConfig> tencentCos = new ArrayList<>();
    /**
     * 百度云 BOS
     */
    private List<? extends BaiduBosConfig> baiduBos = new ArrayList<>();
    /**
     * 又拍云 USS
     */
    private List<? extends UpyunUssConfig> upyunUss = new ArrayList<>();
    /**
     * MinIO USS
     */
    private List<? extends MinioConfig> minio = new ArrayList<>();

    /**
     * Amazon S3
     */
    private List<? extends AmazonS3Config> amazonS3 = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends FtpConfig> ftp = new ArrayList<>();

    /**
     * FTP
     */
    private List<? extends SftpConfig> sftp = new ArrayList<>();

    /**
     * WebDAV
     */
    private List<? extends WebDavConfig> WebDav = new ArrayList<>();

    /**
     * 谷歌云存储
     */
    private List<? extends GoogleCloudStorageConfig> googleCloudStorage = new ArrayList<>();

    /**
     * 基本的存储平台配置
     */
    @Data
    public static class BaseConfig {
        /**
         * 存储平台
         */
        private String platform = "";
    }

    /**
     * 本地存储
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class LocalConfig extends BaseConfig {
        /**
         * 本地存储路径
         */
        private String basePath = "";
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 本地存储升级版
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class LocalPlusConfig extends BaseConfig {
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 华为云 OBS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HuaweiObsConfig extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.HuaweiObsACL}
         */
        private String defaultAcl;
        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 阿里云 OSS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AliyunOssConfig extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.AliyunOssACL}
         */
        private String defaultAcl;
        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 七牛云 Kodo
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class QiniuKodoConfig extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 腾讯云 COS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TencentCosConfig extends BaseConfig {
        private String secretId;
        private String secretKey;
        private String region;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.TencentCosACL}
         */
        private String defaultAcl;
        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 百度云 BOS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class BaiduBosConfig extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.BaiduBosACL}
         */
        private String defaultAcl;
        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 又拍云 USS
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UpyunUssConfig extends BaseConfig {
        private String username;
        private String password;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * MinIO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MinioConfig extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * Amazon S3
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AmazonS3Config extends BaseConfig {
        private String accessKey;
        private String secretKey;
        private String region;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.AwsS3ACL}
         */
        private String defaultAcl;
        /**
         * 自动分片上传阈值，超过此大小则使用分片上传，默认 128MB
         */
        private int multipartThreshold = 128 * 1024 * 1024;
        /**
         * 自动分片上传时每个分片大小，默认 32MB
         */
        private int multipartPartSize = 32 * 1024 * 1024;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * FTP
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FtpConfig extends BaseConfig {
        /**
         * 主机
         */
        private String host;
        /**
         * 端口，默认21
         */
        private int port = 21;
        /**
         * 用户名，默认 anonymous（匿名）
         */
        private String user = "anonymous";
        /**
         * 密码，默认空
         */
        private String password = "";
        /**
         * 编码，默认UTF-8
         */
        private Charset charset = StandardCharsets.UTF_8;
        /**
         * 连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setConnectTimeout(int)}
         */
        private long connectionTimeout = 10 * 1000;
        /**
         * Socket连接超时时长，单位毫秒，默认10秒 {@link org.apache.commons.net.SocketClient#setSoTimeout(int)}
         */
        private long soTimeout = 10 * 1000;
        /**
         * 设置服务器语言，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#setServerLanguageCode(String)}
         */
        private String serverLanguageCode;
        /**
         * 服务器标识，默认空，{@link org.apache.commons.net.ftp.FTPClientConfig#FTPClientConfig(String)}
         * 例如：org.apache.commons.net.ftp.FTPClientConfig.SYST_NT
         */
        private String systemKey;
        /**
         * 是否主动模式，默认被动模式
         */
        private Boolean isActive = false;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";
        /**
         * Client 对象池配置
         */
        private CommonClientPoolConfig pool = new CommonClientPoolConfig();
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * SFTP
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SftpConfig extends BaseConfig {
        /**
         * 主机
         */
        private String host;
        /**
         * 端口，默认22
         */
        private int port = 22;
        /**
         * 用户名
         */
        private String user;
        /**
         * 密码
         */
        private String password;
        /**
         * 私钥路径
         */
        private String privateKeyPath;
        /**
         * 编码，默认UTF-8
         */
        private Charset charset = StandardCharsets.UTF_8;
        /**
         * 连接超时时长，单位毫秒，默认10秒
         */
        private int connectionTimeout = 10 * 1000;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";
        /**
         * Client 对象池配置
         */
        private CommonClientPoolConfig pool = new CommonClientPoolConfig();
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * WebDAV
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WebDavConfig extends BaseConfig {
        /**
         * 服务器地址，注意“/”结尾，例如：http://192.168.1.105:8405/
         */
        private String server;
        /**
         * 用户名
         */
        private String user;
        /**
         * 密码
         */
        private String password;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 存储路径，上传的文件都会存储在这个路径下面，默认“/”，注意“/”结尾
         */
        private String storagePath = "/";
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GoogleCloudStorageConfig extends BaseConfig {
        private String projectId;
        /**
         * 证书路径，兼容Spring的ClassPath路径、文件路径、HTTP路径等
         */
        private String credentialsPath;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 基础路径
         */
        private String basePath = "";
        /**
         * 默认的 ACL，详情 {@link Constant.GoogleCloudStorageACL}
         */
        private String defaultAcl;
        /**
         * 其它自定义配置
         */
        private Map<String,Object> attr = new LinkedHashMap<>();
    }

    /**
     * 通用的 Client 对象池配置，详情见 {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig}
     */
    @Data
    public static class CommonClientPoolConfig {
        /**
         * 取出对象前进行校验，默认开启
         */
        private Boolean testOnBorrow = true;
        /**
         * 空闲检测，默认开启
         */
        private Boolean testWhileIdle = true;
        /**
         * 最大总数量，超过此数量会进行阻塞等待，默认 16
         */
        private Integer maxTotal = 16;
        /**
         * 最大空闲数量，默认 4
         */
        private Integer maxIdle = 4;
        /**
         * 最小空闲数量，默认 1
         */
        private Integer minIdle = 1;
        /**
         * 空闲对象逐出（销毁）运行间隔时间，默认 30 秒
         */
        private Duration timeBetweenEvictionRuns = Duration.ofSeconds(30);
        /**
         * 对象空闲超过此时间将逐出（销毁），为负数则关闭此功能，默认 -1
         */
        private Duration minEvictableIdleDuration = Duration.ofMillis(-1);
        /**
         * 对象空闲超过此时间且当前对象池的空闲对象数大于最小空闲数量，将逐出（销毁），为负数则关闭此功能，默认 30 分钟
         */
        private Duration softMinEvictableIdleDuration = Duration.ofMillis(30);

        public <T> GenericObjectPoolConfig<T> toGenericObjectPoolConfig() {
            GenericObjectPoolConfig<T> config = new GenericObjectPoolConfig<>();
            config.setTestOnBorrow(testOnBorrow);
            config.setTestWhileIdle(testWhileIdle);
            config.setMaxTotal(maxTotal);
            config.setMinIdle(minIdle);
            config.setMaxIdle(maxIdle);
            config.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
            config.setMinEvictableIdleTime(minEvictableIdleDuration);
            config.setSoftMinEvictableIdleTime(softMinEvictableIdleDuration);
            return config;
        }
    }
}
