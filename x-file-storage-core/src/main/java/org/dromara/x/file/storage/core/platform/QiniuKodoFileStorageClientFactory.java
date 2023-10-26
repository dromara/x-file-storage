package org.dromara.x.file.storage.core.platform;

import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.QiniuKodoConfig;
import org.dromara.x.file.storage.core.platform.QiniuKodoFileStorageClientFactory.QiniuKodoClient;

/**
 * 七牛云 Kodo 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class QiniuKodoFileStorageClientFactory implements FileStorageClientFactory<QiniuKodoClient> {
    private String platform;
    private String accessKey;
    private String secretKey;
    private volatile QiniuKodoClient client;

    public QiniuKodoFileStorageClientFactory(QiniuKodoConfig config) {
        platform = config.getPlatform();
        accessKey = config.getAccessKey();
        secretKey = config.getSecretKey();
    }

    @Override
    public QiniuKodoClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new QiniuKodoClient(accessKey, secretKey);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        client = null;
    }

    @Getter
    @Setter
    public static class QiniuKodoClient {
        private String accessKey;
        private String secretKey;
        private volatile Auth auth;
        private volatile Configuration configuration;
        private volatile BucketManager bucketManager;
        private volatile UploadManager uploadManager;

        public QiniuKodoClient(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        public Auth getAuth() {
            if (auth == null) {
                synchronized (this) {
                    if (auth == null) {
                        auth = Auth.create(accessKey, secretKey);
                    }
                }
            }
            return auth;
        }

        public Configuration getConfiguration() {
            if (configuration == null) {
                synchronized (this) {
                    if (configuration == null) {
                        configuration = new Configuration(Region.autoRegion());
                        configuration.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
                    }
                }
            }
            return configuration;
        }

        public BucketManager getBucketManager() {
            if (bucketManager == null) {
                synchronized (this) {
                    if (bucketManager == null) {
                        bucketManager = new BucketManager(getAuth(), getConfiguration());
                    }
                }
            }
            return bucketManager;
        }

        public UploadManager getUploadManager() {
            if (uploadManager == null) {
                synchronized (this) {
                    if (uploadManager == null) {
                        uploadManager = new UploadManager(getConfiguration());
                    }
                }
            }
            return uploadManager;
        }
    }
}
