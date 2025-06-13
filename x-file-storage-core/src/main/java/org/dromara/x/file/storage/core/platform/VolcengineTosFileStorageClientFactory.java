package org.dromara.x.file.storage.core.platform;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.VolcengineTosConfig;

/**
 * 火山引擎 TOS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class VolcengineTosFileStorageClientFactory implements FileStorageClientFactory<TOSV2> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private String region;
    private volatile TOSV2 client;

    public VolcengineTosFileStorageClientFactory(VolcengineTosConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        endPoint = config.getEndPoint();
        region = config.getRegion();
    }

    @Override
    public TOSV2 getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new TOSV2ClientBuilder().build(region, endPoint, accessKey, secretKey);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client = null;
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
        }
    }
}
