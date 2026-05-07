package org.dromara.x.file.storage.core.platform;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.AliyunOssConfig;

/**
 * 阿里云 OSS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class AliyunOssFileStorageClientFactory implements FileStorageClientFactory<OSS> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private RefreshableClient<OSS> refreshableClient;

    public AliyunOssFileStorageClientFactory(AliyunOssConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        endPoint = config.getEndPoint();

        // 使用 {@link RefreshableClient} 包装 OSSClient：进程触发过 OOM 等场景导致 SDK 内部
        //  Apache HttpClient 连接池被永久 shutdown 后，再次调用会抛 {@code IllegalStateException}:
        // "Connection pool shut down"，包装类会自动失效并重建客户端，避免后续请求全部失败。
        // 参考 <a href="https://github.com/dromara/x-file-storage/issues/331">GitHub Issue #331</a>
        refreshableClient = new RefreshableClient<>(
                OSS.class,
                () -> new OSSClientBuilder().build(endPoint, accessKey, secretKey),
                OSS::shutdown,
                RefreshableClient::isApacheHttpClientPoolShutdown,
                () -> "阿里云 OSS 平台 [" + platform + "] 连接池");
    }

    @Override
    public OSS getClient() {
        return refreshableClient.get();
    }

    @Override
    public void close() {
        refreshableClient.close();
    }
}
