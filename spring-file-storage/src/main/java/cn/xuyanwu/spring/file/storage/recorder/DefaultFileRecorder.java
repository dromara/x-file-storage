package cn.xuyanwu.spring.file.storage.recorder;

import cn.xuyanwu.spring.file.storage.FileInfo;

/**
 * 默认的文件记录者类，此类并不能真正保存、查询、删除记录，只是用来脱离数据库运行，保证文件上传功能可以正常使用
 */
public class DefaultFileRecorder implements FileRecorder {
    @Override
    public boolean record(FileInfo fileInfo) {
        return true;
    }

    @Override
    public FileInfo getByUrl(String url) {
        return null;
    }

    @Override
    public boolean delete(String url) {
        return true;
    }
}
