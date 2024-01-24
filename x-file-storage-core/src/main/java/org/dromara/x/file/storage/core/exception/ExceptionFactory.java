package org.dromara.x.file.storage.core.exception;

import cn.hutool.core.util.StrUtil;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;

/**
 * 异常工厂，用于生成各种常用异常，主要用于存储平台的实现类中
 */
public class ExceptionFactory {
    public static final String UPLOAD_MESSAGE_FORMAT = "文件上传失败！platform：{}，filename：{}";
    public static final String UPLOAD_NOT_SUPPORT_ACL_MESSAGE_FORMAT = "文件上传失败，当前存储平台不支持 ALC！platform：{}，fileInfo：{}";
    public static final String UPLOAD_NOT_SUPPORT_METADATA_MESSAGE_FORMAT =
            "文件上传失败，当前存储平台不支持 Metadata！platform：{}，fileInfo：{}";
    public static final String UPLOAD_REQUIRE_SIZE_MESSAGE_FORMAT = "文件上传失败，当前存储平台需要传入文件大小！platform：{}，fileInfo：{}";
    public static final String INITIATE_MULTIPART_UPLOAD_MESSAGE_FORMAT = "手动文件分片上传-初始化失败！platform：{}，fileInfo：{}";
    public static final String INITIATE_MULTIPART_UPLOAD_REQUIRE_SIZE_MESSAGE_FORMAT =
            "手动文件分片上传-初始化失败，当前存储平台需要传入文件大小！platform：{}，fileInfo：{}";
    public static final String INITIATE_MULTIPART_UPLOAD_RECORDER_SAVE_MESSAGE_FORMAT =
            "手动文件分片上传-初始化失败，文件记录保存失败！platform：{}，fileInfo：{}";
    public static final String UPLOAD_PART_MESSAGE_FORMAT = "手动文件分片上传-上传分片失败！platform：{}，fileInfo：{}";
    public static final String COMPLETE_MULTIPART_UPLOAD_MESSAGE_FORMAT = "手动文件分片上传-完成失败！platform：{}，fileInfo：{}";
    public static final String ABORT_MULTIPART_UPLOAD_MESSAGE_FORMAT = "手动文件分片上传-取消失败！platform：{}，fileInfo：{}";
    public static final String LIST_PARTS_MESSAGE_FORMAT = "手动文件分片上传-列举已上传的分片失败！platform：{}，fileInfo：{}";
    public static final String LIST_FILES_MESSAGE_FORMAT =
            "列举文件失败！platform：{}，basePath：{}，path：{}，filenamePrefix：{}，maxFiles：{}，marker：{}";
    public static final String UNRECOGNIZED_ACL_MESSAGE_FORMAT = "无法识别此 ACL！platform：{}，ACL：{}";
    public static final String GENERATE_PRESIGNED_URL_MESSAGE_FORMAT = "对文件生成可以签名访问的 URL 失败！platform：{}，fileInfo：{}";
    public static final String GENERATE_TH_PRESIGNED_URL_MESSAGE_FORMAT =
            "对缩略图文件生成可以签名访问的 URL 失败！platform：{}，fileInfo：{}";
    public static final String SET_FILE_ACL_MESSAGE_FORMAT = "设置文件的 ACL 失败！platform：{}，fileInfo：{}，ACL：{}";
    public static final String SET_TH_FILE_ACL_MESSAGE_FORMAT = "设置缩略图文件的 ACL 失败！platform：{}，fileInfo：{}，ACL：{}";
    public static final String DELETE_MESSAGE_FORMAT = "文件删除失败！platform：{}，filename：{}";
    public static final String EXISTS_MESSAGE_FORMAT = "查询文件是否存在失败！platform：{}，filename：{}";
    public static final String DOWNLOAD_MESSAGE_FORMAT = "文件下载失败！platform：{},fileInfo：{}";
    public static final String DOWNLOAD_TH_MESSAGE_FORMAT = "缩略图文件下载失败！platform：{},fileInfo：{}";
    public static final String DOWNLOAD_TH_NOT_FOUND_MESSAGE_FORMAT = "缩略图文件下载失败，文件不存在！platform：{},fileInfo：{}";
    public static final String SAME_COPY_NOT_SUPPORT_ACL_MESSAGE_FORMAT =
            "同存储平台复制文件失败，当前存储平台不支持 ALC！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_NOT_SUPPORT_METADATA_MESSAGE_FORMAT =
            "同存储平台复制文件失败，当前存储平台不支持 Metadata！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_BASE_PATH_MESSAGE_FORMAT =
            "同存储平台复制文件失败，源文件 basePath：{} 与当前存储平台的 basePath：{} 不同！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_NOT_FOUND_MESSAGE_FORMAT =
            "同存储平台复制文件失败，无法获取源文件信息！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_CREATE_PATH_MESSAGE_FORMAT =
            "同存储平台复制文件失败，无法创建目标路径！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_TH_MESSAGE_FORMAT =
            "同存储平台复制文件失败，缩略图文件复制失败！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_COPY_MESSAGE_FORMAT = "同存储平台复制文件失败！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_NOT_SUPPORT_ACL_MESSAGE_FORMAT =
            "同存储平台移动文件失败，当前存储平台不支持 ALC！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_NOT_SUPPORT_METADATA_MESSAGE_FORMAT =
            "同存储平台移动文件失败，当前存储平台不支持 Metadata！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_BASE_PATH_MESSAGE_FORMAT =
            "同存储平台移动文件失败，源文件 basePath：{} 与当前存储平台的 basePath：{} 不同！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_NOT_FOUND_MESSAGE_FORMAT =
            "同存储平台移动文件失败，无法获取源文件信息！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_CREATE_PATH_MESSAGE_FORMAT =
            "同存储平台移动文件失败，无法创建目标路径！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_TH_MESSAGE_FORMAT =
            "同存储平台移动文件失败，缩略图文件移动失败！platform：{}，srcFileInfo：{}，destFileInfo：{}";
    public static final String SAME_MOVE_MESSAGE_FORMAT = "同存储平台移动文件失败！platform：{}，srcFileInfo：{}，destFileInfo：{}";

    /**
     * 上传异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     */
    public static FileStorageRuntimeException upload(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(UPLOAD_MESSAGE_FORMAT, fileInfo.getOriginalFilename(), platform), e);
    }

    /**
     * 上传时，此存储平台不支持 ACL 异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException uploadNotSupportAcl(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(UPLOAD_NOT_SUPPORT_ACL_MESSAGE_FORMAT, platform, fileInfo));
    }

    /**
     * 上传时，此存储平台不支持 Metadata 异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException uploadNotSupportMetadata(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(UPLOAD_NOT_SUPPORT_METADATA_MESSAGE_FORMAT, platform, fileInfo));
    }

    /**
     * 上传时，未传入文件大小异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException uploadRequireFileSize(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(StrUtil.format(UPLOAD_REQUIRE_SIZE_MESSAGE_FORMAT, platform, fileInfo));
    }

    /**
     * 手动分片上传-初始化异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException initiateMultipartUpload(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(INITIATE_MULTIPART_UPLOAD_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 手动分片上传时，未传入文件大小异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException initiateMultipartUploadRequireFileSize(
            FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(StrUtil.format(UPLOAD_REQUIRE_SIZE_MESSAGE_FORMAT, platform, fileInfo));
    }

    /**
     * 手动分片上传-初始化失败，文件记录保存失败异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException initiateMultipartUploadRecorderSave(
            FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(INITIATE_MULTIPART_UPLOAD_RECORDER_SAVE_MESSAGE_FORMAT, platform, fileInfo), e);
    }
    /**
     * 手动分片上传-上传分片异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException uploadPart(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(StrUtil.format(UPLOAD_PART_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 手动分片上传-完成异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException completeMultipartUpload(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(COMPLETE_MULTIPART_UPLOAD_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 手动分片上传-取消异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException abortMultipartUpload(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(ABORT_MULTIPART_UPLOAD_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 手动分片上传-列举已上传的分片
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException listParts(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(StrUtil.format(LIST_PARTS_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 列举文件
     * @param pre 预处理器
     * @param basePath 基础路径
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException listFiles(ListFilesPretreatment pre, String basePath, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(
                        LIST_PARTS_MESSAGE_FORMAT,
                        pre.getPlatform(),
                        basePath,
                        pre.getPath(),
                        pre.getFilenamePrefix(),
                        pre.getMaxFiles(),
                        pre.getMarker()),
                e);
    }

    /**
     * 无法识别此 ACL 异常
     * @param acl ALC（访问控制列表）
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException unrecognizedAcl(Object acl, String platform) {
        return new FileStorageRuntimeException(StrUtil.format(UNRECOGNIZED_ACL_MESSAGE_FORMAT, platform, acl));
    }

    /**
     * 对文件生成可以签名访问的 URL 异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException generatePresignedUrl(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(GENERATE_PRESIGNED_URL_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL 异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException generateThPresignedUrl(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(GENERATE_TH_PRESIGNED_URL_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 设置文件的 ALC 异常
     * @param fileInfo 文件信息
     * @param acl ALC（访问控制列表）
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException setFileAcl(FileInfo fileInfo, Object acl, String platform, Exception e) {
        return new FileStorageRuntimeException(StrUtil.format(SET_FILE_ACL_MESSAGE_FORMAT, platform, fileInfo, acl), e);
    }

    /**
     * 设置缩略图文件的 ALC 异常
     * @param fileInfo 文件信息
     * @param acl ALC（访问控制列表）
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException setThFileAcl(
            FileInfo fileInfo, Object acl, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SET_TH_FILE_ACL_MESSAGE_FORMAT, platform, fileInfo, acl), e);
    }

    /**
     * 删除异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     */
    public static FileStorageRuntimeException delete(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(DELETE_MESSAGE_FORMAT, platform, fileInfo.getOriginalFilename()), e);
    }

    /**
     * 是否存在
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     */
    public static FileStorageRuntimeException exists(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(EXISTS_MESSAGE_FORMAT, platform, fileInfo.getOriginalFilename()), e);
    }

    /**
     * 下载文件异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException download(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(StrUtil.format(DOWNLOAD_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 下载缩略图异常
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException downloadTh(FileInfo fileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(StrUtil.format(DOWNLOAD_TH_MESSAGE_FORMAT, platform, fileInfo), e);
    }

    /**
     * 下载缩略图异常，文件不存在
     * @param fileInfo 文件信息
     * @param platform 存储平台名称
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException downloadThNotFound(FileInfo fileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(DOWNLOAD_TH_NOT_FOUND_MESSAGE_FORMAT, platform, fileInfo));
    }

    /**
     * 同存储平台复制文件时，此存储平台不支持 ACL 异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException sameCopyNotSupportAcl(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_NOT_SUPPORT_ACL_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo));
    }

    /**
     * 同存储平台复制文件时，此存储平台不支持 Metadata 异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException sameCopyNotSupportMetadata(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_NOT_SUPPORT_METADATA_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo));
    }

    /**
     * 同存储平台复制文件时，源文件 basePath 与当前存储平台的 basePath 不一致异常
     *
     * @param basePath 此存储平台的基础路径
     * @param srcFileInfo  源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform     存储平台名称
     */
    public static FileStorageRuntimeException sameCopyBasePath(
            String basePath, FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(StrUtil.format(
                SAME_COPY_BASE_PATH_MESSAGE_FORMAT,
                srcFileInfo.getBasePath(),
                basePath,
                platform,
                srcFileInfo,
                destFileInfo));
    }

    /**
     * 同存储平台复制文件异常，源文件不存在
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameCopyNotFound(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_NOT_FOUND_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台复制文件异常，无法创建目标路径
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameCopyCreatePath(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_CREATE_PATH_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台复制文件异常，缩略图文件复制失败
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameCopyTh(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_TH_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台复制文件异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameCopy(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_COPY_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台移动文件时，此存储平台不支持 ACL 异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException sameMoveNotSupportAcl(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_NOT_SUPPORT_ACL_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo));
    }

    /**
     * 同存储平台移动文件时，此存储平台不支持 Metadata 异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     */
    public static FileStorageRuntimeException sameMoveNotSupportMetadata(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_NOT_SUPPORT_METADATA_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo));
    }

    /**
     * 同存储平台移动文件时，源文件 basePath 与当前存储平台的 basePath 不一致异常
     *
     * @param basePath 此存储平台的基础路径
     * @param srcFileInfo  源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform     存储平台名称
     */
    public static FileStorageRuntimeException sameMoveBasePath(
            String basePath, FileInfo srcFileInfo, FileInfo destFileInfo, String platform) {
        return new FileStorageRuntimeException(StrUtil.format(
                SAME_MOVE_BASE_PATH_MESSAGE_FORMAT,
                srcFileInfo.getBasePath(),
                basePath,
                platform,
                srcFileInfo,
                destFileInfo));
    }

    /**
     * 同存储平台移动文件异常，源文件不存在
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameMoveNotFound(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_NOT_FOUND_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台移动文件异常，无法创建目标路径
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameMoveCreatePath(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_CREATE_PATH_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台移动文件异常，缩略图文件移动失败
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameMoveTh(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_TH_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }

    /**
     * 同存储平台移动文件异常
     * @param srcFileInfo 源文件消息
     * @param destFileInfo 目标文件信息
     * @param platform 存储平台名称
     * @param e 源异常
     * @return {@link FileStorageRuntimeException}
     */
    public static FileStorageRuntimeException sameMove(
            FileInfo srcFileInfo, FileInfo destFileInfo, String platform, Exception e) {
        return new FileStorageRuntimeException(
                StrUtil.format(SAME_MOVE_MESSAGE_FORMAT, platform, srcFileInfo, destFileInfo), e);
    }
}
