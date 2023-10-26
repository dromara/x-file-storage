package org.dromara.x.file.storage.core.platform;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.MinioConfig;

/**
 * MinIO 存储平台的 Client 工厂
 */
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
                    client = new MinioClient.Builder()
                            .credentials(accessKey, secretKey)
                            .endpoint(endPoint)
                            .build();
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
