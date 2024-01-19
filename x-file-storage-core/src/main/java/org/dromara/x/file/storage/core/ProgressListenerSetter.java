package org.dromara.x.file.storage.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.dromara.x.file.storage.core.util.Tools;

public interface ProgressListenerSetter<T extends ProgressListenerSetter<?>> {

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供一个参数，表示已传输字节数
     */
    @Deprecated
    default T setProgressMonitor(boolean flag, Consumer<Long> progressMonitor) {
        return setProgressListener(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供一个参数，表示已传输字节数
     */
    @Deprecated
    default T setProgressMonitor(Consumer<Long> progressMonitor) {
        return setProgressListener(progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Deprecated
    default T setProgressMonitor(boolean flag, BiConsumer<Long, Long> progressMonitor) {
        return setProgressListener(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Deprecated
    default T setProgressMonitor(BiConsumer<Long, Long> progressMonitor) {
        return setProgressListener(progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     */
    @Deprecated
    default T setProgressMonitor(boolean flag, ProgressListener progressMonitor) {
        return setProgressListener(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     */
    @Deprecated
    default T setProgressMonitor(ProgressListener progressMonitor) {
        return setProgressListener(progressMonitor);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    default T setProgressListener(boolean flag, Consumer<Long> progressListener) {
        if (flag) setProgressListener(progressListener);
        return Tools.cast(this);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    default T setProgressListener(Consumer<Long> progressListener) {
        return setProgressListener((progressSize, allSize) -> progressListener.accept(progressSize));
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    default T setProgressListener(boolean flag, BiConsumer<Long, Long> progressListener) {
        if (flag) setProgressListener(progressListener);
        return Tools.cast(this);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    default T setProgressListener(BiConsumer<Long, Long> progressListener) {
        return setProgressListener(new ProgressListener() {
            @Override
            public void start() {}

            @Override
            public void progress(long progressSize, Long allSize) {
                progressListener.accept(progressSize, allSize);
            }

            @Override
            public void finish() {}
        });
    }

    /**
     * 设置进度监听器
     */
    default T setProgressListener(boolean flag, ProgressListener progressListener) {
        if (flag) setProgressListener(progressListener);
        return Tools.cast(this);
    }

    /**
     * 设置进度监听器
     */
    T setProgressListener(ProgressListener progressListener);
}
