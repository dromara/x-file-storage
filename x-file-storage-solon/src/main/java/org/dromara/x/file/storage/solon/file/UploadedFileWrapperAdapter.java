package org.dromara.x.file.storage.solon.file;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.noear.solon.core.handle.UploadedFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Solon UploadedFile 文件包装适配器
 */
@Getter
@Setter
public class UploadedFileWrapperAdapter implements FileWrapperAdapter {

    @Override
    public boolean isSupport(final Object source) {
        return source instanceof UploadedFile || source instanceof UploadedFileWrapper;
    }


    @Override
    public FileWrapper getFileWrapper(final Object source, final String name, final String contentType, final Long size) throws IOException {
        if (source instanceof UploadedFileWrapper) {
            return updateFileWrapper((UploadedFileWrapper) source, name, contentType, size);
        } else {
            UploadedFile file = (UploadedFile) source;
            return new UploadedFileWrapper(file,
                    Optional.ofNullable(name).orElseGet(file::getName),
                    Optional.ofNullable(contentType).orElseGet(file::getContentType),
                    Optional.ofNullable(size).orElseGet(file::getContentSize));
        }
    }
}
