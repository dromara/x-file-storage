package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

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
     * 是否支持手动分片上传
     */
    default MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.notSupport();
    }

    /**
     * 手动分片上传-初始化
     */
    default void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {}

    /**
     * 手动分片上传-上传分片
     */
    default FilePartInfo uploadPart(UploadPartPretreatment pre) {
        return null;
    }

    /**
     * 手动分片上传-完成
     */
    default void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {}

    /**
     * 手动分片上传-取消
     */
    default void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {}

    /**
     * 手动分片上传-列举已上传的分片
     */
    default FilePartInfoList listParts(ListPartsPretreatment pre) {
        return null;
    }

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
    default void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {}

    /**
     * 是否支持同存储平台移动文件
     */
    default boolean isSupportSameMove() {
        return false;
    }

    /**
     * 同存储平台移动文件
     */
    default void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {}

    /**
     * 释放相关资源
     */
    default void close() {}

    /**
     * 获取文件全路径（相对存储平台的存储路径）
     *
     * @param fileInfo 文件信息
     */
    default String getFileKey(FileInfo fileInfo) {
        return Tools.getNotNull(fileInfo.getBasePath(), StrUtil.EMPTY)
                + Tools.getNotNull(fileInfo.getPath(), StrUtil.EMPTY)
                + Tools.getNotNull(fileInfo.getFilename(), StrUtil.EMPTY);
    }
    /**
     * 获取缩略图全路径（相对存储平台的存储路径）
     *
     * @param fileInfo 文件信息
     */
    default String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return Tools.getNotNull(fileInfo.getBasePath(), StrUtil.EMPTY)
                + Tools.getNotNull(fileInfo.getPath(), StrUtil.EMPTY)
                + Tools.getNotNull(fileInfo.getThFilename(), StrUtil.EMPTY);
    }
}
