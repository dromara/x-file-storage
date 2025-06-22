package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.qiniu.storage.*;
import java.net.URI;
import lombok.*;
import org.dromara.x.file.storage.core.FileStorageProperties.AmazonS3V2Config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Amazon S3 存储平台的 Client 工厂<br/>
 * 适用于AWS SDK for Java 2.x，根据 Amazon 公告，AWS SDK for Java 1.x 自 2024 年 7 月 31 日起将进入维护模式，并于 2025 年 12 月 31 日终止支持<br/>
 * 公告链接地址：<a href="https://aws.amazon.com/blogs/developer/the-aws-sdk-for-java-1-x-is-in-maintenance-mode-effective-july-31-2024/">The AWS SDK for Java 1.x is in maintenance mode, effective July 31, 2024</a>
 * @author zhangxin
 * @date 2024-12-08
 */
@Getter
@Setter
@NoArgsConstructor
public class AmazonS3V2FileStorageClientFactory
        implements FileStorageClientFactory<AmazonS3V2FileStorageClientFactory.AmazonS3V2Client> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endPoint;
    private volatile AmazonS3V2Client client;

    public AmazonS3V2FileStorageClientFactory(AmazonS3V2Config config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        region = config.getRegion();
        endPoint = config.getEndPoint();
    }

    @Override
    public AmazonS3V2Client getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new AmazonS3V2Client(accessKey, secretKey, region, endPoint);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Getter
    @Setter
    public static class AmazonS3V2Client implements AutoCloseable {
        private String accessKey;
        private String secretKey;
        private String region;
        private String endPoint;
        private volatile S3Client client;
        private volatile S3Presigner presigner;

        public AmazonS3V2Client(String accessKey, String secretKey, String region, String endPoint) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.region = region;
            this.endPoint = endPoint;
        }

        public S3Client getClient() {
            if (client == null) {
                synchronized (this) {
                    if (client == null) {
                        S3ClientBuilder builder = S3Client.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(accessKey, secretKey)))
                                .region(Region.of(region));
                        if (StrUtil.isNotBlank(endPoint)) {
                            builder.endpointOverride(URI.create(endPoint));
                        }
                        client = builder.build();
                    }
                }
            }
            return client;
        }

        public S3Presigner getPresigner() {
            if (presigner == null) {
                synchronized (this) {
                    if (presigner == null) {
                        S3Presigner.Builder builder = S3Presigner.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(accessKey, secretKey)))
                                .region(Region.of(region));
                        if (StrUtil.isNotBlank(endPoint)) {
                            builder.endpointOverride(URI.create(endPoint));
                        }
                        presigner = builder.build();
                    }
                }
            }
            return presigner;
        }

        @Override
        public void close() {
            if (client != null) {
                client.close();
                client = null;
            }
            if (presigner != null) {
                presigner.close();
                presigner = null;
            }
        }
    }
}
