package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.URLUtil;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.IOException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.WebDavConfig;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

/**
 * WebDAV 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class WebDavFileStorageClientFactory implements FileStorageClientFactory<Sardine> {
    private String platform;
    private String server;
    private String user;
    private String password;
    private volatile Sardine client;

    public WebDavFileStorageClientFactory(WebDavConfig config) {
        platform = config.getPlatform();
        server = config.getServer();
        user = config.getUser();
        password = config.getPassword();
    }

    @Override
    public Sardine getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = SardineFactory.begin(user, password);
                    client.enablePreemptiveAuthentication(URLUtil.url(server));
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client.shutdown();
            } catch (IOException e) {
                throw new FileStorageRuntimeException("关闭 WebDAV Client 失败！", e);
            }
            client = null;
        }
    }
}
