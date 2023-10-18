package org.dromara.x.file.storage.spring.file;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Multipart 文件包装适配器
 */
@Getter
@Setter
public class MultipartFileWrapperAdapter implements FileWrapperAdapter {

    @Override
    public boolean isSupport(Object source) {
        return source instanceof MultipartFile || source instanceof MultipartFileWrapper;
    }

    @Override
    public FileWrapper getFileWrapper(Object source,String name,String contentType,Long size) throws IOException {
        if (source instanceof MultipartFileWrapper) {
            return updateFileWrapper((MultipartFileWrapper) source,name,contentType,size);
        } else {
            MultipartFile file = (MultipartFile) source;
            if (name == null) name = file.getOriginalFilename();
            if (contentType == null) contentType = file.getContentType();
            if (size == null) size = file.getSize();
            return new MultipartFileWrapper(file,name,contentType,size);
        }
    }
}
