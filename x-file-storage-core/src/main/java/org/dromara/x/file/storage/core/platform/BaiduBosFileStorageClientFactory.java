package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.baidubce.Protocol;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.BaiduBosConfig;

/**
 * 百度云 BOS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class BaiduBosFileStorageClientFactory implements FileStorageClientFactory<BosClient> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private String endPoint;
    private volatile BosClient client;

    public BaiduBosFileStorageClientFactory(BaiduBosConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
        endPoint = config.getEndPoint();
    }

    @Override
    public BosClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    BosClientConfiguration configuration = new BosClientConfiguration();
                    configuration.setProtocol(Protocol.HTTPS);
                    configuration.setCredentials(new DefaultBceCredentials(accessKey,secretKey));
                    if (StrUtil.isNotBlank(endPoint)) {
                        configuration.setEndpoint(endPoint);
                    }
                    client = new BosClient(configuration);
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
