package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.URLUtil;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.GoogleCloudStorageConfig;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

/**
 * GoogleCloud Storage 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class GoogleCloudStorageFileStorageClientFactory implements FileStorageClientFactory<Storage> {
    private String platform;
    private String projectId;
    private String credentialsPath;
    private volatile Storage client;

    public GoogleCloudStorageFileStorageClientFactory(GoogleCloudStorageConfig config) {
        platform = config.getPlatform();
        projectId = config.getProjectId();
        credentialsPath = config.getCredentialsPath();
    }

    @Override
    public Storage getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ServiceAccountCredentials credentialsFromStream;
                    try (InputStream in = URLUtil.url(credentialsPath).openStream()) {
                        credentialsFromStream = ServiceAccountCredentials.fromStream(in);
                    } catch (IOException e) {
                        throw new FileStorageRuntimeException(
                                "GoogleCloud Storage Platform 授权 key 文件获取失败！credentialsPath：" + credentialsPath);
                    }
                    List<String> scopes = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
                    ServiceAccountCredentials credentials =
                            credentialsFromStream.toBuilder().setScopes(scopes).build();
                    StorageOptions storageOptions = StorageOptions.newBuilder()
                            .setProjectId(projectId)
                            .setCredentials(credentials)
                            .build();
                    client = storageOptions.getService();
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                throw new FileStorageRuntimeException("关闭 GoogleCloud Storage Client 失败！", e);
            }
            client = null;
        }
    }
}
