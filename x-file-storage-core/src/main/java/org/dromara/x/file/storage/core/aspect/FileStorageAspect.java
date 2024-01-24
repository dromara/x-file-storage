package org.dromara.x.file.storage.core.aspect;

import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.get.FileFileInfoList;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.get.ListFilesSupportInfo;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MultipartUploadSupportInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.*;

/**
 * 文件服务切面接口，用来干预文件上传，删除等
 */
public interface FileStorageAspect {

    /**
     * 上传，成功返回文件信息，失败返回 null
     */
    default FileInfo uploadAround(
            UploadAspectChain chain,
            FileInfo fileInfo,
            UploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(fileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 是否支持手动分片上传
     */
    default MultipartUploadSupportInfo isSupportMultipartUpload(
            IsSupportMultipartUploadAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 手动分片上传-初始化，成功返回文件信息，失败返回 null
     */
    default FileInfo initiateMultipartUploadAround(
            InitiateMultipartUploadAspectChain chain,
            FileInfo fileInfo,
            InitiateMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(fileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 手动分片上传-上传分片，成功返回文件信息
     */
    default FilePartInfo uploadPart(
            UploadPartAspectChain chain,
            UploadPartPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(pre, fileStorage, fileRecorder);
    }

    /**
     * 手动分片上传-完成
     */
    default FileInfo completeMultipartUploadAround(
            CompleteMultipartUploadAspectChain chain,
            CompleteMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            ContentTypeDetect contentTypeDetect) {
        return chain.next(pre, fileStorage, fileRecorder, contentTypeDetect);
    }

    /**
     * 手动分片上传-取消
     */
    default FileInfo abortMultipartUploadAround(
            AbortMultipartUploadAspectChain chain,
            AbortMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(pre, fileStorage, fileRecorder);
    }

    /**
     * 手动分片上传-列举已上传的分片
     */
    default FilePartInfoList listParts(ListPartsAspectChain chain, ListPartsPretreatment pre, FileStorage fileStorage) {
        return chain.next(pre, fileStorage);
    }

    /**
     * 是否支持手列举文件
     */
    default ListFilesSupportInfo isSupportListFiles(IsSupportListFilesAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 列举文件
     */
    default FileFileInfoList listFiles(ListFilesAspectChain chain, ListFilesPretreatment pre, FileStorage fileStorage) {
        return chain.next(pre, fileStorage);
    }

    /**
     * 删除文件，成功返回 true
     */
    default boolean deleteAround(
            DeleteAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder) {
        return chain.next(fileInfo, fileStorage, fileRecorder);
    }

    /**
     * 文件是否存在，成功返回 true
     */
    default boolean existsAround(ExistsAspectChain chain, FileInfo fileInfo, FileStorage fileStorage) {
        return chain.next(fileInfo, fileStorage);
    }

    /**
     * 下载文件，成功返回文件内容
     */
    default void downloadAround(
            DownloadAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        chain.next(fileInfo, fileStorage, consumer);
    }

    /**
     * 下载缩略图文件，成功返回文件内容
     */
    default void downloadThAround(
            DownloadThAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        chain.next(fileInfo, fileStorage, consumer);
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    default boolean isSupportPresignedUrlAround(IsSupportPresignedUrlAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     */
    default String generatePresignedUrlAround(
            GeneratePresignedUrlAspectChain chain, FileInfo fileInfo, Date expiration, FileStorage fileStorage) {
        return chain.next(fileInfo, expiration, fileStorage);
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     */
    default String generateThPresignedUrlAround(
            GenerateThPresignedUrlAspectChain chain, FileInfo fileInfo, Date expiration, FileStorage fileStorage) {
        return chain.next(fileInfo, expiration, fileStorage);
    }

    /**
     * 是否支持文件的访问控制列表
     */
    default boolean isSupportAclAround(IsSupportAclAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setFileAcl(SetFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        return chain.next(fileInfo, acl, fileStorage);
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    default boolean setThFileAcl(
            SetThFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        return chain.next(fileInfo, acl, fileStorage);
    }

    /**
     * 是否支持 Metadata
     */
    default boolean isSupportMetadataAround(IsSupportMetadataAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 是否支持同存储平台复制
     */
    default boolean isSupportSameCopyAround(IsSupportSameCopyAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 复制，成功返回文件信息
     */
    default FileInfo copyAround(
            CopyAspectChain chain,
            FileInfo srcFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(srcFileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 同存储平台复制，成功返回文件信息
     */
    default FileInfo sameCopyAround(
            SameCopyAspectChain chain,
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 是否支持同存储平台移动
     */
    default boolean isSupportSameMoveAround(IsSupportSameMoveAspectChain chain, FileStorage fileStorage) {
        return chain.next(fileStorage);
    }

    /**
     * 移动，成功返回文件信息
     */
    default FileInfo moveAround(
            MoveAspectChain chain,
            FileInfo srcFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(srcFileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 同存储平台移动，成功返回文件信息
     */
    default FileInfo sameMoveAround(
            SameMoveAspectChain chain,
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        return chain.next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 通过反射调用指定存储平台的方法
     */
    default <T> T invoke(InvokeAspectChain chain, FileStorage fileStorage, String method, Object[] args) {
        return chain.next(fileStorage, method, args);
    }
}
