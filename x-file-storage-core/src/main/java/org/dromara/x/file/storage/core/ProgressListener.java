package org.dromara.x.file.storage.core;

/**
 * 进度监听器
 */
public interface ProgressListener {

    /**
     * 开始
     */
    void start();

    /**
     * 进行中
     *
     * @param progressSize 已经进行的大小
     * @param allSize      总大小，来自 fileInfo.getSize()，未知大小的流可能会导致此参数为 null
     */
    void progress(long progressSize,Long allSize);

    /**
     * 结束
     */
    void finish();
}
