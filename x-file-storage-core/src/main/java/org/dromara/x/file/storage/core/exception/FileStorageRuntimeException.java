package org.dromara.x.file.storage.core.exception;

import cn.hutool.core.util.StrUtil;
import org.dromara.x.file.storage.core.FileInfo;

/**
 * FileStorage 运行时异常
 */
public class FileStorageRuntimeException extends RuntimeException {

    private static final String SAVE_MESSAGE_FORMAT = "文件上传失败！platform：{}，filename：{}";

    private static final String DELETE_MESSAGE_FORMAT = "文件删除失败！platform：{}，filename：{}";

    private static final String EXISTS_MESSAGE_FORMAT = "查询文件是否存在失败！platform：{}，filename：{}";

    private static final String ACL_MESSAGE_FORMAT = "文件上传失败，FTP 不支持设置 ACL！platform：{}，filename：{}";

    private static final String DOWNLOAD_MESSAGE_FORMAT = "文件下载失败！platform：{},fileInfo：{}";

    private static final String DOWNLOAD_TH_MESSAGE_FORMAT = "缩略图文件下载失败！platform：{},fileInfo：{}";

    private static final String DOWNLOAD_TH_NOT_FOUND_MESSAGE_FORMAT = "缩略图文件下载失败，文件不存在！platform：{},fileInfo：{}";

    public FileStorageRuntimeException() {}

    public FileStorageRuntimeException(String message) {
        super(message);
    }

    public FileStorageRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageRuntimeException(Throwable cause) {
        super(cause);
    }

    public FileStorageRuntimeException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 保存异常
     *
     * @param fileInfo
     * @param platform
     * @param e
     */
    public static FileStorageRuntimeException save(FileInfo fileInfo, String platform, Throwable e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAVE_MESSAGE_FORMAT, fileInfo.getOriginalFilename(), platform), e);
    }

    /**
     * 删除异常
     *
     * @param fileInfo
     * @param platform
     * @param e
     */
    public static FileStorageRuntimeException delete(FileInfo fileInfo, String platform, Throwable e) {
        return new FileStorageRuntimeException(
                StrUtil.format(DELETE_MESSAGE_FORMAT, platform, fileInfo.getOriginalFilename()), e);
    }

    /**
     * 是否存在
     *
     * @param fileInfo
     * @param platform
     * @param e
     */
    public static FileStorageRuntimeException exists(FileInfo fileInfo, String platform, Throwable e) {
        return new FileStorageRuntimeException(
                StrUtil.format(EXISTS_MESSAGE_FORMAT, platform, fileInfo.getOriginalFilename()), e);
    }

    /**
     * @param fileInfo
     * @param platform
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException acl(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(ACL_MESSAGE_FORMAT, fileInfo.getOriginalFilename(), platform));
    }

    /**
     * 下载文件异常
     *
     * @param fileInfo
     * @param platform
     * @param e
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException download(FileInfo fileInfo, String platform, Throwable e) {
        return new FileStorageRuntimeException(StrUtil.format(DOWNLOAD_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 下载缩略图异常
     *
     * @param fileInfo
     * @param platform
     * @param e
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException downloadTh(FileInfo fileInfo, String platform, Throwable e) {
        return new FileStorageRuntimeException(StrUtil.format(DOWNLOAD_TH_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 下载缩略图异常，文件不存在
     *
     * @param fileInfo
     * @param platform
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException downloadThNotFound(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(DOWNLOAD_TH_NOT_FOUND_MESSAGE_FORMAT, platform, fileInfo));
    }
}
