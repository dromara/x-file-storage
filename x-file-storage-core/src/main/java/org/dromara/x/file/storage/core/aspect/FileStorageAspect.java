package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 文件服务切面接口，用来干预文件上传，删除等
 */
public interface FileStorageAspect {


    /**
     * 上传，成功返回文件信息，失败返回 null
     */
    default FileInfo uploadAround(UploadAspectChain chain,FileInfo fileInfo,UploadPretreatment pre,FileStorage fileStorage,FileRecorder fileRecorder) {
        return chain.next(fileInfo,pre,fileStorage,fileRecorder);
    }

    /**
     * 删除文件，成功返回 true
     */
    default boolean deleteAround(DeleteAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,FileRecorder fileRecorder) {
        return chain.next(fileInfo,fileStorage,fileRecorder);
    }

    /**
     * 文件是否存在，成功返回 true
     */
    default boolean existsAround(ExistsAspectChain chain,FileInfo fileInfo,FileStorage fileStorage) {
        return chain.next(fileInfo,fileStorage);
    }

    /**
     * 下载文件，成功返回文件内容
     */
    default void downloadAround(DownloadAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        chain.next(fileInfo,fileStorage,consumer);
    }

    /**
     * 下载缩略图文件，成功返回文件内容
     */
    default void downloadThAround(DownloadThAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        chain.next(fileInfo,fileStorage,consumer);
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    default boolean isSupportPresignedUrlAround(IsSupportPresignedUrlAspectChain chain,FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     */
    default String generatePresignedUrlAround(GeneratePresignedUrlAspectChain chain,FileInfo fileInfo,Date expiration,FileStorage fileStorage) {
        return chain.next(fileInfo,expiration,fileStorage);
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     */
    default String generateThPresignedUrlAround(GenerateThPresignedUrlAspectChain chain,FileInfo fileInfo,Date expiration,FileStorage fileStorage) {
        return chain.next(fileInfo,expiration,fileStorage);
    }

    /**
     * 是否支持文件的访问控制列表
     */
    default boolean isSupportAclAround(IsSupportAclAspectChain chain,FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setFileAcl(SetFileAclAspectChain chain,FileInfo fileInfo,Object acl,FileStorage fileStorage) {
        return chain.next(fileInfo,acl,fileStorage);
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setThFileAcl(SetThFileAclAspectChain chain,FileInfo fileInfo,Object acl,FileStorage fileStorage) {
        return chain.next(fileInfo,acl,fileStorage);
    }

    /**
     * 是否支持 Metadata
     */
    default boolean isSupportMetadataAround(IsSupportMetadataAspectChain chain,FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 通过反射调用指定存储平台的方法
     */
    default <T> T invoke(InvokeAspectChain chain,FileStorage fileStorage,String method,Object[] args) {
        return chain.next(fileStorage,method,args);
    }
}
