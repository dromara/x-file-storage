package org.dromara.x.file.storage.core.recorder;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

/**
 * 默认的文件记录者类，此类并不能真正保存、查询、删除记录，只是用来脱离数据库运行，保证文件上传功能可以正常使用
 */
public class DefaultFileRecorder implements FileRecorder {
    @Override
    public boolean save(FileInfo fileInfo) {
        return true;
    }

    @Override
    public FileInfo getByUrl(String url) {
        throw new FileStorageRuntimeException("尚未实现 FileRecorder 接口，暂时无法使用此功能，参考文档：https://x-file-storage.xuyanwu.cn/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95");
    }

    @Override
    public boolean delete(String url) {
        throw new FileStorageRuntimeException("尚未实现 FileRecorder 接口，暂时无法使用此功能，参考文档：https://x-file-storage.xuyanwu.cn/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95");
    }
}
