package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.storage.*;
import com.qiniu.util.Auth;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import lombok.*;
import org.dromara.x.file.storage.core.FileStorageProperties.QiniuKodoConfig;
import org.dromara.x.file.storage.core.platform.QiniuKodoFileStorageClientFactory.QiniuKodoClient;
import org.dromara.x.file.storage.core.util.Tools;

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
        private volatile Client client;
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

        public Client getClient() {
            if (client == null) {
                synchronized (this) {
                    if (client == null) {
                        client = new Client(getConfiguration());
                    }
                }
            }
            return client;
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
                        bucketManager = new BucketManager(getAuth(), getConfiguration(), getClient());
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

        public <T> UploadActionResult<T> retryUploadAction(UploadAction<T> action, String token)
                throws InvocationTargetException, InstantiationException, IllegalAccessException {
            Class<?> uploadTokenClass = ClassUtil.loadClass("com.qiniu.storage.UploadToken");
            Class<?> resumeUploadSourceStreamClass = ClassUtil.loadClass("com.qiniu.storage.ResumeUploadSourceStream");
            Class<?> resumeUploadPerformerV2Class = ClassUtil.loadClass("com.qiniu.storage.ResumeUploadPerformerV2");
            Object uploadToken = ReflectUtil.newInstance(uploadTokenClass, token);
            Object v2 = ReflectUtil.getConstructor(
                            resumeUploadPerformerV2Class,
                            Client.class,
                            String.class,
                            uploadTokenClass,
                            resumeUploadSourceStreamClass,
                            Recorder.class,
                            UploadOptions.class,
                            Configuration.class)
                    .newInstance(
                            getClient(),
                            null,
                            uploadToken,
                            null,
                            null,
                            new UploadOptions.Builder().build(),
                            getConfiguration());

            Class<?> uploadActionClass = ClassUtil.loadClass("com.qiniu.storage.ResumeUploadPerformer.UploadAction");
            final Object[] resultWrapper = new Object[1];
            Object uploadAction = Proxy.newProxyInstance(
                    this.getClass().getClassLoader(), new Class[] {uploadActionClass}, (proxy, method, args) -> {
                        if ("uploadAction".equals(method.getName()) && args.length == 1 && args[0] instanceof String) {
                            UploadActionResult<T> result = action.uploadAction((String) args[0]);
                            resultWrapper[0] = result;
                            return result.getResponse();
                        }
                        return method.invoke(proxy, args);
                    });
            ReflectUtil.invoke(v2, "retryUploadAction", uploadAction);
            return Tools.cast(resultWrapper[0]);
        }

        public interface UploadAction<T> {
            UploadActionResult<T> uploadAction(String host) throws QiniuException;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UploadActionResult<T> {
            private Response response;
            private T data;
        }
    }
}
