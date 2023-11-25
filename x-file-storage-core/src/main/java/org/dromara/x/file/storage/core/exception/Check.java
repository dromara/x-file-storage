package org.dromara.x.file.storage.core.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.move.MovePretreatment;

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
}
