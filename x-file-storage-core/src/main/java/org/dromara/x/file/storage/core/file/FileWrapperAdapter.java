package org.dromara.x.file.storage.core.file;

import java.io.IOException;

/**
 * 文件包装适配器接口
 */
public interface FileWrapperAdapter {

    /**
     * 是否支持
     */
    boolean isSupport(Object source);

    /**
     * 获取文件包装
     */
    FileWrapper getFileWrapper(Object source,String name,String contentType,Long size) throws IOException;


    /**
     * 更新文件包装参数
     */
    default FileWrapper updateFileWrapper(FileWrapper fileWrapper,String name,String contentType,Long size) {
        if (name != null) fileWrapper.setName(name);
        if (contentType != null) fileWrapper.setContentType(contentType);
        if (size != null) fileWrapper.setSize(size);
        return fileWrapper;
    }
}
