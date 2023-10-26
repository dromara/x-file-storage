package org.dromara.x.file.storage.core.platform;

/**
 * 存储平台的 Client 的对象的工厂接口
 */
public interface FileStorageClientFactory<Client> extends AutoCloseable {

    /**
     * 获取平台
     */
    String getPlatform();

    /**
     * 获取 Client ，部分存储平台例如 FTP 、 SFTP 使用完后需要归还
     */
    Client getClient();

    /**
     * 归还 Client
     */
    default void returnClient(Client client) {}

    /**
     * 释放相关资源
     */
    @Override
    default void close() {}
}
