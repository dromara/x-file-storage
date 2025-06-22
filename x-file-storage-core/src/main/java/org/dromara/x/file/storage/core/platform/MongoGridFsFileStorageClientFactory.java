package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.IoUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileStorageProperties.MongoGridFsConfig;
import org.dromara.x.file.storage.core.platform.MongoGridFsFileStorageClientFactory.MongoGridFsClient;

/**
 * Mongo GridFS 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class MongoGridFsFileStorageClientFactory implements FileStorageClientFactory<MongoGridFsClient> {
    private String platform = "";
    private String connectionString;
    private String database;
    private String bucketName;
    private volatile MongoGridFsClient client;

    public MongoGridFsFileStorageClientFactory(MongoGridFsConfig config) {
        platform = config.getPlatform();
        connectionString = config.getConnectionString();
        database = config.getDatabase();
        bucketName = config.getBucketName();
    }

    @Override
    public MongoGridFsClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new MongoGridFsClient(connectionString, database, bucketName);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            IoUtil.close(client);
            client = null;
        }
    }

    @Getter
    @Setter
    public static class MongoGridFsClient implements AutoCloseable {
        private String connectionString;
        private String database;
        private String bucketName;
        private volatile MongoClient mongoClient;
        private volatile GridFSBucket gridFsBucket;

        public MongoGridFsClient(String connectionString, String database, String bucketName) {
            this.connectionString = connectionString;
            this.database = database;
            this.bucketName = bucketName;
        }

        public MongoClient getMongoClient() {
            if (mongoClient == null) {
                synchronized (this) {
                    if (mongoClient == null) {
                        mongoClient = MongoClients.create(connectionString);
                    }
                }
            }
            return mongoClient;
        }

        public GridFSBucket getGridFsBucket() {
            if (gridFsBucket == null) {
                synchronized (this) {
                    if (gridFsBucket == null) {
                        gridFsBucket = GridFSBuckets.create(getMongoClient().getDatabase(database), bucketName);
                    }
                }
            }
            return gridFsBucket;
        }

        @Override
        public void close() throws Exception {
            mongoClient.close();
        }
    }
}
