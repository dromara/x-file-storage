package org.dromara.x.file.storage.core.platform;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.AzureBlobStorageConfig;
import org.dromara.x.file.storage.core.platform.AzureBlobStorageFileStorageClientFactory.AzureBlobStorageClient;

@Data
public class AzureBlobStorageFileStorageClientFactory implements FileStorageClientFactory<AzureBlobStorageClient> {

    private AzureBlobStorageConfig config;
    private volatile AzureBlobStorageClient client;

    public AzureBlobStorageFileStorageClientFactory(AzureBlobStorageConfig config) {
        this.config = config;
    }

    @Override
    public String getPlatform() {
        return config.getPlatform();
    }

    @Override
    public AzureBlobStorageClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new AzureBlobStorageClient(config);
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
    public static final class AzureBlobStorageClient {
        private AzureBlobStorageConfig config;
        private volatile BlobServiceClient blobServiceClient;
        private volatile DataLakeServiceClient dataLakeServiceClient;

        public AzureBlobStorageClient(AzureBlobStorageConfig config) {
            this.config = config;
        }

        public BlobServiceClient getBlobServiceClient() {
            if (blobServiceClient == null) {
                synchronized (this) {
                    if (blobServiceClient == null) {
                        blobServiceClient = new BlobServiceClientBuilder()
                                .endpoint(config.getEndPoint())
                                .connectionString(config.getConnectionString())
                                .buildClient();
                    }
                }
            }
            return blobServiceClient;
        }

        public DataLakeServiceClient getDataLakeServiceClient() {
            if (dataLakeServiceClient == null) {
                synchronized (this) {
                    if (dataLakeServiceClient == null) {
                        dataLakeServiceClient = new DataLakeServiceClientBuilder()
                                .endpoint(config.getEndPoint())
                                .connectionString(config.getConnectionString())
                                .buildClient();
                    }
                }
            }
            return dataLakeServiceClient;
        }
    }
}
