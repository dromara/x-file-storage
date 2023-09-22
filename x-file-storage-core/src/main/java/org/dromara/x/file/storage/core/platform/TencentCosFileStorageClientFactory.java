package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.TencentCosConfig;

/**
 * 腾讯云 COS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class TencentCosFileStorageClientFactory implements FileStorageClientFactory<COSClient> {
    private String platform;
    private String secretId;
    private String secretKey;
    private String region;
    private volatile COSClient client;

    public TencentCosFileStorageClientFactory(TencentCosConfig config) {
        platform = config.getPlatform();
        secretId = config.getSecretId();
        secretKey = config.getSecretKey();
        region = config.getRegion();
    }

    @Override
    public COSClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ClientConfig clientConfig = new ClientConfig();
                    if (StrUtil.isNotBlank(region)) {
                        clientConfig.setRegion(new Region(region));
                    }
                    client = new COSClient(new BasicCOSCredentials(secretId,secretKey),clientConfig);
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
