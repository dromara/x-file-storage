package org.dromara.x.file.storage.core.recorder;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.upload.FilePartInfo;

/**
 * 默认的文件记录者类，此类并不能真正保存、查询、删除记录，只是用来脱离数据库运行，保证文件上传功能可以正常使用
 */
public class DefaultFileRecorder implements FileRecorder {
    @Override
    public boolean save(FileInfo fileInfo) {
        return true;
    }

    public void update(FileInfo fileInfo) {}

    @Override
    public FileInfo getByUrl(String url) {
        throw new FileStorageRuntimeException(
                "尚未实现 FileRecorder 接口，暂时无法使用此功能，可以参考文档快速入门的其它操作章节，或者参考保存上传记录章节：https://x-file-storage.xuyanwu.cn/2.3.0/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95");
    }

    @Override
    public boolean delete(String url) {
        return true;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {}

    @Override
    public void deleteFilePartByUploadId(String uploadId) {}
}
