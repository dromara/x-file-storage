package cn.xuyanwu.spring.file.storage;

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
     * @param allSize      总大小，来自 fileInfo.getSize()
     */
    void progress(long progressSize,long allSize);

    /**
     * 结束
     */
    void finish();
}
