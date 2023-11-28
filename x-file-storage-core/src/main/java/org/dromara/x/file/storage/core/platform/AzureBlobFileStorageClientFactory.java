package org.dromara.x.file.storage.core.platform;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.Data;
import org.dromara.x.file.storage.core.FileStorageProperties.AzureBlobStorageConfig;

@Data
public class AzureBlobFileStorageClientFactory implements FileStorageClientFactory<BlobServiceClient> {

    private String platform;

    /**
     * blob 服务终节点，国区 https://<storage-account-name>.blob.core.chinacloudapi.cn
     */
    private String endpoint;


    /**
     * 连接字符串，凭证
     */
    private String connectionString;

    private volatile BlobServiceClient client;


    public AzureBlobFileStorageClientFactory(AzureBlobStorageConfig config) {
        this.platform = config.getPlatform();
        this.endpoint = config.getEndPoint();
        this.connectionString = config.getConnectionString();

    }

    @Override
    public BlobServiceClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new BlobServiceClientBuilder()
                            .endpoint(endpoint)
                            .connectionString(connectionString)
                            .buildClient();
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
