package org.dromara.x.file.storage.core.recorder;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.upload.FilePartInfo;

/**
 * 文件记录记录者接口，参考文档：https://x-file-storage.xuyanwu.cn/2.3.0/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95
 */
public interface FileRecorder {

    /**
     * 保存文件记录
     */
    boolean save(FileInfo fileInfo);

    /**
     * 更新文件记录，可以根据文件 ID 或 URL 来更新文件记录，
     * 主要用在手动分片上传文件-完成上传，作用是更新文件信息
     */
    void update(FileInfo fileInfo);

    /**
     * 根据 url 获取文件记录
     */
    FileInfo getByUrl(String url);

    /**
     * 根据 url 删除文件记录
     */
    boolean delete(String url);

    /**
     * 保存文件分片信息
     * @param filePartInfo 文件分片信息
     */
    void saveFilePart(FilePartInfo filePartInfo);

    /**
     * 删除文件分片信息
     */
    void deleteFilePartByUploadId(String uploadId);
}
