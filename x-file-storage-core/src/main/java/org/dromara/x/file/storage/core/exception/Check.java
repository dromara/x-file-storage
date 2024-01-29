package org.dromara.x.file.storage.core.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;

/**
 * 用于检查条件并抛出对应异常，主要用于存储平台的实现类中
 */
public class Check {

    /**
     * 上传文件时，检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportAcl(String platform, FileInfo fileInfo, UploadPretreatment pre) {
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw ExceptionFactory.uploadNotSupportAcl(fileInfo, platform);
        }
    }

    /**
     * 上传文件时，检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportAcl(
            String platform, FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw ExceptionFactory.uploadNotSupportAcl(fileInfo, platform);
        }
    }

    /**
     * 上传文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportMetadata(String platform, FileInfo fileInfo, UploadPretreatment pre) {
        if ((CollUtil.isNotEmpty(fileInfo.getMetadata()) || CollUtil.isNotEmpty(fileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw ExceptionFactory.uploadNotSupportMetadata(fileInfo, platform);
        }
    }

    /**
     * 上传文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportMetadata(
            String platform, FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        if ((CollUtil.isNotEmpty(fileInfo.getMetadata()) || CollUtil.isNotEmpty(fileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw ExceptionFactory.uploadNotSupportMetadata(fileInfo, platform);
        }
    }

    /**
     * 上传文件时，检查是否传入文件大小，如果未传入则抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     */
    public static void uploadRequireFileSize(String platform, FileInfo fileInfo) {
        if (fileInfo.getSize() == null) {
            throw ExceptionFactory.uploadRequireFileSize(fileInfo, platform);
        }
    }

    /**
     * 手动分片上传时，检查是否传入文件大小，如果未传入则抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     */
    public static void initiateMultipartUploadRequireFileSize(String platform, FileInfo fileInfo) {
        if (fileInfo.getSize() == null) {
            throw ExceptionFactory.initiateMultipartUploadRequireFileSize(fileInfo, platform);
        }
    }

    /**
     * 下载文件缩略图时，检查是否传入缩略图文件名，如果没有则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     */
    public static void downloadThBlankThFilename(String platform, FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw ExceptionFactory.downloadThNotFound(fileInfo, platform);
        }
    }

    /**
     * 同存储平台复制文件时，检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameCopyNotSupportAcl(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        if (srcFileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw ExceptionFactory.sameCopyNotSupportAcl(srcFileInfo, destFileInfo, platform);
        }
    }
    /**
     * 同存储平台复制文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameCopyNotSupportMetadata(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        if ((CollUtil.isNotEmpty(srcFileInfo.getMetadata()) || CollUtil.isNotEmpty(srcFileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw ExceptionFactory.sameCopyNotSupportMetadata(srcFileInfo, destFileInfo, platform);
        }
    }

    /**
     * 同存储平台复制文件时，检查源文件 basePath 与当前存储平台的 basePath 是否一致，不一致则抛出异常
     * @param platform 存储平台名称
     * @param basePath 存储平台的基础路径
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     */
    public static void sameCopyBasePath(String platform, String basePath, FileInfo srcFileInfo, FileInfo destFileInfo) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw ExceptionFactory.sameCopyBasePath(basePath, srcFileInfo, destFileInfo, platform);
        }
    }

    /**
     * 同存储平台移动文件检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameMoveNotSupportAcl(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        if (srcFileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw ExceptionFactory.sameMoveNotSupportAcl(srcFileInfo, destFileInfo, platform);
        }
    }
    /**
     * 同存储平台移动文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameMoveNotSupportMetadata(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        if ((CollUtil.isNotEmpty(srcFileInfo.getMetadata()) || CollUtil.isNotEmpty(srcFileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw ExceptionFactory.sameMoveNotSupportMetadata(srcFileInfo, destFileInfo, platform);
        }
    }

    /**
     * 同存储平台移动文件时，检查源文件 basePath 与当前存储平台的 basePath 是否一致，不一致则抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     */
    public static void sameMoveBasePath(String platform, String basePath, FileInfo srcFileInfo, FileInfo destFileInfo) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw ExceptionFactory.sameMoveBasePath(basePath, srcFileInfo, destFileInfo, platform);
        }
    }

    /**
     * 手动分片上传-上传分片时，检查文件信息相关参数，如果缺少则抛出异常
     * @param fileInfo 文件信息
     */
    public static void uploadPart(FileInfo fileInfo) {
        if (fileInfo == null) throw new FileStorageRuntimeException("手动分片上传-上传分片失败，请传入 fileInfo 参数");
        if (fileInfo.getPlatform() == null)
            throw new FileStorageRuntimeException("手动分片上传-上传分片失败，请在 FileInfo 中传入 platform 参数");
        if (fileInfo.getBasePath() == null)
            throw new FileStorageRuntimeException("手动分片上传-上传分片失败，请在 FileInfo 中传入 basePath 参数");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("手动分片上传-上传分片失败，请在 FileInfo 中传入 path 参数");
        if (fileInfo.getFilename() == null)
            throw new FileStorageRuntimeException("手动分片上传-上传分片失败，请在 FileInfo 中传入 filename 参数");
        if (fileInfo.getUploadId() == null) throw new RuntimeException("手动分片上传-上传分片失败，请在 FileInfo 中传入 uploadId 参数");
    }

    /**
     * 手动分片上传-完成时，检查文件信息相关参数，如果缺少则抛出异常
     * @param fileInfo 文件信息
     */
    public static void completeMultipartUpload(FileInfo fileInfo) {
        if (fileInfo == null) throw new FileStorageRuntimeException("手动分片上传-完成失败，请传入 fileInfo 参数");
        if (fileInfo.getPlatform() == null)
            throw new FileStorageRuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 platform 参数");
        if (fileInfo.getBasePath() == null)
            throw new FileStorageRuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 basePath 参数");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 path 参数");
        if (fileInfo.getFilename() == null)
            throw new FileStorageRuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 filename 参数");
        if (fileInfo.getId() == null && fileInfo.getUrl() == null)
            throw new RuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 id 或 url 参数");
        if (fileInfo.getUploadId() == null) throw new RuntimeException("手动分片上传-完成失败，请在 FileInfo 中传入 uploadId 参数");
    }

    /**
     * 手动分片上传-取消时，检查文件信息相关参数，如果缺少则抛出异常
     * @param fileInfo 文件信息
     */
    public static void abortMultipartUpload(FileInfo fileInfo) {
        if (fileInfo == null) throw new FileStorageRuntimeException("手动分片上传-取消失败，请传入 fileInfo 参数");
        if (fileInfo.getPlatform() == null)
            throw new FileStorageRuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 platform 参数");
        if (fileInfo.getBasePath() == null)
            throw new FileStorageRuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 basePath 参数");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 path 参数");
        if (fileInfo.getFilename() == null)
            throw new FileStorageRuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 filename 参数");
        if (fileInfo.getUrl() == null) throw new FileStorageRuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 url 参数");
        if (fileInfo.getUploadId() == null) throw new RuntimeException("手动分片上传-取消失败，请在 FileInfo 中传入 uploadId 参数");
    }

    /**
     * 手动分片上传-列举已上传的分片时，检查文件信息相关参数，如果缺少则抛出异常
     * @param fileInfo 文件信息
     */
    public static void listParts(FileInfo fileInfo) {
        if (fileInfo == null) throw new FileStorageRuntimeException("手动分片上传-列举已上传的分片失败，请传入 fileInfo 参数");
        if (fileInfo.getPlatform() == null)
            throw new FileStorageRuntimeException("手动分片上传-列举已上传的分片失败，请在 FileInfo 中传入 platform 参数");
        if (fileInfo.getBasePath() == null)
            throw new FileStorageRuntimeException("手动分片上传-列举已上传的分片失败，请在 FileInfo 中传入 basePath 参数");
        if (fileInfo.getPath() == null)
            throw new FileStorageRuntimeException("手动分片上传-列举已上传的分片失败，请在 FileInfo 中传入 path 参数");
        if (fileInfo.getFilename() == null)
            throw new FileStorageRuntimeException("手动分片上传-列举已上传的分片失败，请在 FileInfo 中传入 filename 参数");
        if (fileInfo.getUploadId() == null) throw new RuntimeException("手动分片上传-列举已上传的分片失败，请在 FileInfo 中传入 uploadId 参数");
    }

    /**
     * 列举文件时，检查文件信息相关参数，如果缺少则抛出异常
     * @param pre 列举文件预处理器
     */
    public static void listFiles(ListFilesPretreatment pre) {
        if (pre.getPlatform() == null) throw new FileStorageRuntimeException("列举文件失败，请传入 platform 参数");
        if (pre.getPath() == null) throw new FileStorageRuntimeException("列举文件失败，请传入 path 参数");
        if (pre.getFilenamePrefix() == null) throw new FileStorageRuntimeException("列举文件失败，请传入 filenamePrefix 参数");
    }

    /**
     * 获取文件时，检查文件信息相关参数，如果缺少则抛出异常
     * @param pre 列举文件预处理器
     */
    public static void getFile(GetFilePretreatment pre) {
        if (pre.getPlatform() == null) throw new FileStorageRuntimeException("获取文件失败，请传入 platform 参数");
        if (pre.getPath() == null) throw new FileStorageRuntimeException("获取文件失败，请传入 path 参数");
        if (pre.getFilename() == null) throw new FileStorageRuntimeException("获取文件失败，请传入 filename 参数");
        // if (pre.getUrl() == null) throw new FileStorageRuntimeException("获取文件失败，请传入 url 参数");
    }
}
