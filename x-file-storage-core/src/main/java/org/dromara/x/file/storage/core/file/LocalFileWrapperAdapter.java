package org.dromara.x.file.storage.core.file;

import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;

/**
 * 本地文件包装适配器
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileWrapperAdapter implements FileWrapperAdapter {
    private ContentTypeDetect contentTypeDetect;

    @Override
    public boolean isSupport(Object source) {
        return source instanceof File || source instanceof LocalFileWrapper;
    }

    @Override
    public FileWrapper getFileWrapper(Object source, String name, String contentType, Long size) throws IOException {
        if (source instanceof LocalFileWrapper) {
            return updateFileWrapper((LocalFileWrapper) source, name, contentType, size);
        } else {
            File file = (File) source;
            if (name == null) name = file.getName();
            if (contentType == null) contentType = contentTypeDetect.detect(file);
            if (size == null) size = file.length();
            return new LocalFileWrapper(file, name, contentType, size);
        }
    }
}
