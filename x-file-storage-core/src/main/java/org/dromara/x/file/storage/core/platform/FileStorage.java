package org.dromara.x.file.storage.core.platform;

import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;

/**
 * 文件存储接口，对应各个平台
 */
public interface FileStorage extends AutoCloseable {

    /**
     * 获取平台
     */
    String getPlatform();

    /**
     * 设置平台
     */
    void setPlatform(String platform);

    /**
     * 保存文件
     */
    boolean save(FileInfo fileInfo, UploadPretreatment pre);

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    default boolean isSupportPresignedUrl() {
        return false;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    default String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        return null;
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    default String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        return null;
    }

    /**
     * 是否支持文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean isSupportAcl() {
        return false;
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setFileAcl(FileInfo fileInfo, Object acl) {
        return false;
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        return false;
    }

    /**
     * 是否支持 Metadata，一般情况下只有对象存储支持该功能
     */
    default boolean isSupportMetadata() {
        return false;
    }

    /**
     * 删除文件
     */
    boolean delete(FileInfo fileInfo);

    /**
     * 文件是否存在
     */
    boolean exists(FileInfo fileInfo);

    /**
     * 下载文件
     */
    void download(FileInfo fileInfo, Consumer<InputStream> consumer);

    /**
     * 下载缩略图文件
     */
    void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer);

    /**
     * 是否支持同存储平台复制文件
     */
    default boolean isSupportSameCopy() {
        return false;
    }

    /**
     * 同存储平台复制文件
     */
    default void sameCopy(FileInfo srcFileInfo,FileInfo destFileInfo,CopyPretreatment pre) {}

    /**
     * 释放相关资源
     */
    default void close() {}
}
