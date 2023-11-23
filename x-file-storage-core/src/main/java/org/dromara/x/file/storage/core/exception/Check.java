package org.dromara.x.file.storage.core.exception;

import cn.hutool.core.collection.CollUtil;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.move.MovePretreatment;

/**
 * 用于检查条件并抛出对应异常
 */
public class Check {

    /**
     * 上传文件时，检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportedAcl(String platform, FileInfo fileInfo, UploadPretreatment pre) {
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，【" + platform + "】不支持设置 ACL！，" + fileInfo);
        }
    }
    /**
     * 上传文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param fileInfo 文件信息
     * @param pre 文件上传预处理对象
     */
    public static void uploadNotSupportedMetadata(String platform, FileInfo fileInfo, UploadPretreatment pre) {
        if ((CollUtil.isNotEmpty(fileInfo.getMetadata()) || CollUtil.isNotEmpty(fileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，【" + platform + "】不支持设置 Metadata！，" + fileInfo);
        }
    }

    /**
     * 同存储平台复制文件时，检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameCopyNotSupportedAcl(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        if (srcFileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件复制失败，【" + platform + "】不支持设置 ACL！，srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }
    /**
     * 同存储平台复制文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameCopyNotSupportedMetadata(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        if ((CollUtil.isNotEmpty(srcFileInfo.getMetadata()) || CollUtil.isNotEmpty(srcFileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件复制失败，【" + platform + "】不支持设置 Metadata！，srcFileInfo：" + srcFileInfo
                    + "，destFileInfo：" + destFileInfo);
        }
    }

    /**
     * 同存储平台复制文件时，检查源文件 basePath 与当前存储平台的 basePath 是否一致，不一致则抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     */
    public static void sameCopyBasePath(String platform, String basePath, FileInfo srcFileInfo, FileInfo destFileInfo) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw new FileStorageRuntimeException("文件复制失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath
                    + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }

    /**
     * 同存储平台移动文件检查是否传入 ACL，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameMoveNotSupportedAcl(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        if (srcFileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException(
                    "文件移动失败，【" + platform + "】不支持设置 ACL！，srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }
    /**
     * 同存储平台移动文件时，检查是否传入 Metadata，如果传入则按要求抛出异常
     * @param platform 存储平台名称
     * @param srcFileInfo 源文件信息
     * @param destFileInfo 目标文件信息
     * @param pre 文件上传预处理对象
     */
    public static void sameMoveNotSupportedMetadata(
            String platform, FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        if ((CollUtil.isNotEmpty(srcFileInfo.getMetadata()) || CollUtil.isNotEmpty(srcFileInfo.getUserMetadata()))
                && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件移动失败，【" + platform + "】不支持设置 Metadata！，srcFileInfo：" + srcFileInfo
                    + "，destFileInfo：" + destFileInfo);
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
            throw new FileStorageRuntimeException("文件移动失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath
                    + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }
    }
}
