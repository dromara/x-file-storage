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
                    // 检查配置是否完整
                    if (accessKey == null || accessKey.trim().isEmpty()) {
                        throw new IllegalArgumentException("火山引擎TOS配置错误: accessKey不能为空");
                    }
                    if (secretKey == null || secretKey.trim().isEmpty()) {
                        throw new IllegalArgumentException("火山引擎TOS配置错误: secretKey不能为空");
                    }
                    if (endPoint == null || endPoint.trim().isEmpty()) {
                        throw new IllegalArgumentException("火山引擎TOS配置错误: endPoint不能为空");
                    }
                    if (region == null || region.trim().isEmpty()) {
                        throw new IllegalArgumentException("火山引擎TOS配置错误: region不能为空");
                    }
                    try {
                        client = new TOSV2ClientBuilder().build(region, endPoint, accessKey, secretKey);
                    } catch (Exception e) {
                        throw new RuntimeException("初始化火山引擎TOS客户端失败: " + e.getMessage(), e);
                    }
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
