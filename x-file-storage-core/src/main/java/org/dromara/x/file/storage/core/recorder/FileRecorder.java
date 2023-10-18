package org.dromara.x.file.storage.core.recorder;

import org.dromara.x.file.storage.core.FileInfo;

/**
 * 文件记录记录者接口
 */
public interface FileRecorder {

    /**
     * 保存文件记录
     */
    boolean save(FileInfo fileInfo);

    /**
     * 根据 url 获取文件记录
     */
    FileInfo getByUrl(String url);

    /**
     * 根据 url 删除文件记录
     */
    boolean delete(String url);
}
