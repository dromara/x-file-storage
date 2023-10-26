package org.dromara.x.file.storage.core;

import java.util.function.LongSupplier;

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
    void progress(long progressSize, Long allSize);

    /**
     * 结束
     */
    void finish();

    /**
     * 快速触发开始
     */
    static void quickStart(ProgressListener progressListener, Long size) {
        if (progressListener == null) return;
        progressListener.start();
        progressListener.progress(0, size);
    }

    /**
     * 快速触发结束
     */
    static void quickFinish(ProgressListener progressListener, Long size, LongSupplier progressSizeSupplier) {
        if (progressListener == null) return;
        progressListener.progress(progressSizeSupplier.getAsLong(), size);
        progressListener.finish();
    }

    /**
     * 快速触发结束
     */
    static void quickFinish(ProgressListener progressListener, Long size) {
        if (progressListener == null) return;
        progressListener.progress(size, size);
        progressListener.finish();
    }
}
