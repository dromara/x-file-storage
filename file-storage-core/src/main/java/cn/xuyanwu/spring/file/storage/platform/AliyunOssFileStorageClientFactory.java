package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileStorageProperties.AliyunOssConfig;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private volatile OSS client;

    public AliyunOssFileStorageClientFactory(AliyunOssConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        endPoint = config.getEndPoint();
    }

    @Override
    public OSS getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new OSSClientBuilder().build(endPoint,accessKey,secretKey);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }
}
