package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileStorageProperties.MinioConfig;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * MinIO 存储平台的 Client 工厂
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class MinioFileStorageClientFactory implements FileStorageClientFactory<MinioClient> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private volatile MinioClient client;

    public MinioFileStorageClientFactory(MinioConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        endPoint = config.getEndPoint();
    }

    @Override
    public MinioClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new MinioClient.Builder().credentials(accessKey,secretKey).endpoint(endPoint).build();
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        client = null;
    }
}
