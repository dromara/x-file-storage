package org.dromara.x.file.storage.core.upload;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.file.FileWrapper;

/**
 * 手动分片上传-上传分片预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class UploadPartPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;
    /**
     * 要上传的分片文件包装类
     */
    private FileWrapper partFileWrapper;
    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    private int partNumber;
    /**
     * 上传进度监听
     */
    private ProgressListener progressListener;

    /**
     * 传时用的增强版本的 InputStream ，可以带进度监听、计算哈希等功能，仅内部使用
     */
    private InputStreamPlus inputStreamPlus;

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public UploadPartPretreatment setProgressListener(boolean flag, Consumer<Long> progressListener) {
        if (flag) setProgressListener(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public UploadPartPretreatment setProgressListener(Consumer<Long> progressListener) {
        return setProgressListener((progressSize, allSize) -> progressListener.accept(progressSize));
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public UploadPartPretreatment setProgressListener(boolean flag, BiConsumer<Long, Long> progressListener) {
        if (flag) setProgressListener(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public UploadPartPretreatment setProgressListener(BiConsumer<Long, Long> progressListener) {
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
     * 设置上传进度监听器
     */
    public UploadPartPretreatment setProgressListener(boolean flag, ProgressListener progressListener) {
        if (flag) setProgressListener(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     */
    public UploadPartPretreatment setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    /**
     * 获取增强版本的 InputStream ，可以带进度监听、计算哈希等功能
     */
    public InputStreamPlus getInputStreamPlus() throws IOException {
        return getInputStreamPlus(true);
    }

    /**
     * 获取增强版本的 InputStream ，可以带进度监听、计算哈希等功能
     */
    public InputStreamPlus getInputStreamPlus(boolean hasListener) throws IOException {
        if (inputStreamPlus == null) {
            inputStreamPlus = new InputStreamPlus(
                    partFileWrapper.getInputStream(), hasListener ? progressListener : null, partFileWrapper.getSize());
        }
        return inputStreamPlus;
    }

    /**
     * 执行上传
     */
    public FilePartInfo upload() {
        return new UploadPartActuator(this).execute();
    }
}
