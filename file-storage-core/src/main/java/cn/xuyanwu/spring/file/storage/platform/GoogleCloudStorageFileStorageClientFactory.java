package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.util.URLUtil;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.GoogleCloudStorageConfig;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Google Cloud Storage 存储平台的 Client 工厂
 */
@Slf4j
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
                        throw new FileStorageRuntimeException("Google Cloud Storage Platform 授权 key 文件获取失败！credentialsPath：" + credentialsPath);
                    }
                    List<String> scopes = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
                    ServiceAccountCredentials credentials = credentialsFromStream.toBuilder().setScopes(scopes).build();
                    StorageOptions storageOptions = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build();
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
                throw new FileStorageRuntimeException("关闭 Google Cloud Storage Client 失败！",e);
            }
            client = null;
        }
    }
}
