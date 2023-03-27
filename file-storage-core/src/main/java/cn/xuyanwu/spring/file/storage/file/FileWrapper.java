package cn.xuyanwu.spring.file.storage.file;


import cn.xuyanwu.spring.file.storage.IOExceptionConsumer;
import cn.xuyanwu.spring.file.storage.IOExceptionFunction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件包装接口
 */
public interface FileWrapper extends AutoCloseable {
    /**
     * 获取文件名称
     */
    String getName();

    /**
     * 设置文件名称
     */
    void setName(String name);

    /**
     * 获取文件的 MIME 类型
     */
    String getContentType();

    /**
     * 设置文件的 MIME 类型
     */
    void setContentType(String contentType);

    /**
     * 获取文件的 InputStream
     */
    InputStream getInputStream() throws IOException;

    /**
     * 获取文件的 InputStream 并读取，会自动标记和重置流的位置
     */
    default void getInputStreamMaskReset(IOExceptionConsumer<InputStream> consumer) throws IOException {
        getInputStreamMaskResetReturn(in -> {
            consumer.accept(in);
            return null;
        });
    }

    /**
     * 获取文件的 InputStream 并读取，会自动标记和重置流的位置
     */
    default <R> R getInputStreamMaskResetReturn(IOExceptionFunction<InputStream,R> function) throws IOException {
        InputStream in = getInputStream();
        in.mark(Integer.MAX_VALUE);
        try {
            return function.apply(in);
        } finally {
            in.reset();
        }
    }

    /**
     * 获取文件大小
     */
    Long getSize();


    /**
     * 设置文件大小
     */
    void setSize(Long size);

    /**
     * 移动文件
     */
    void transferTo(File dest);

    /**
     * 是否支持移动文件
     */
    boolean supportTransfer();

}
