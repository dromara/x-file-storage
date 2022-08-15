package cn.xuyanwu.spring.file.storage;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConditionalOnMissingBean(FileStorageProperties.class)
@ConfigurationProperties(prefix = "spring.file-storage")
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
    private List<Local> local = new ArrayList<>();
    /**
     * 华为云 OBS
     */
    private List<HuaweiObs> huaweiObs = new ArrayList<>();
    /**
     * 阿里云 OSS
     */
    private List<AliyunOss> aliyunOss = new ArrayList<>();
    /**
     * 七牛云 Kodo
     */
    private List<QiniuKodo> qiniuKodo = new ArrayList<>();
    /**
     * 腾讯云 COS
     */
    private List<TencentCos> tencentCos = new ArrayList<>();
    /**
     * 百度云 BOS
     */
    private List<BaiduBos> baiduBos = new ArrayList<>();
    /**
     * 又拍云 USS
     */
    private List<UpyunUSS> upyunUSS = new ArrayList<>();
    /**
     * MinIO USS
     */
    private List<MinIO> minio = new ArrayList<>();

    /**
     * AWS S3
     */
    private List<AwsS3> awsS3 = new ArrayList<>();

    /**
     * FTP
     */
    private List<FTP> ftp = new ArrayList<>();

    /**
     * 本地存储
     */
    @Data
    public static class Local {
        /**
         * 本地存储路径
         */
        private String basePath = "";
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
        /**
         * 存储平台
         */
        private String platform = "local";
        /**
         * 访问域名
         */
        private String domain = "";
    }

    /**
     * 华为云 OBS
     */
    @Data
    public static class HuaweiObs {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * 阿里云 OSS
     */
    @Data
    public static class AliyunOss {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * 七牛云 Kodo
     */
    @Data
    public static class QiniuKodo {
        private String accessKey;
        private String secretKey;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * 腾讯云 COS
     */
    @Data
    public static class TencentCos {
        private String secretId;
        private String secretKey;
        private String region;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * 百度云 BOS
     */
    @Data
    public static class BaiduBos {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * 又拍云 USS
     */
    @Data
    public static class UpyunUSS {
        private String username;
        private String password;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * MinIO
     */
    @Data
    public static class MinIO {
        private String accessKey;
        private String secretKey;
        private String endPoint;
        private String bucketName;
        /**
         * 访问域名
         */
        private String domain = "";
        /**
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * AWS S3
     */
    @Data
    public static class AwsS3 {
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
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

    /**
     * FTP
     */
    @Data
    public static class FTP {
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
         * 启用存储
         */
        private Boolean enableStorage = false;
        /**
         * 存储平台
         */
        private String platform = "";
        /**
         * 基础路径
         */
        private String basePath = "";
    }

}
