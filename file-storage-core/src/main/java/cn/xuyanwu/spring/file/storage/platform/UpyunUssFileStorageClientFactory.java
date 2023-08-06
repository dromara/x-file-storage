package cn.xuyanwu.spring.file.storage.platform;

import cn.xuyanwu.spring.file.storage.FileStorageProperties.UpyunUssConfig;
import com.upyun.RestManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 又拍云 USS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class UpyunUssFileStorageClientFactory implements FileStorageClientFactory<RestManager> {
    private String platform;
    private String username;
    private String password;
    private String bucketName;
    private volatile RestManager client;

    public UpyunUssFileStorageClientFactory(UpyunUssConfig config) {
        platform = config.getPlatform();
        username = config.getUsername();
        password = config.getPassword();
        bucketName = config.getBucketName();
    }

    @Override
    public RestManager getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new RestManager(bucketName,username,password);
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
