package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;

import java.io.InputStream;
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
     * 文件是否存在，成功返回文件内容
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
}
