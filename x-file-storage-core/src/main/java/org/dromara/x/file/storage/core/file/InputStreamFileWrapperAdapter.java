package org.dromara.x.file.storage.core.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream 文件包装适配器
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InputStreamFileWrapperAdapter implements FileWrapperAdapter {
    private ContentTypeDetect contentTypeDetect;

    @Override
    public boolean isSupport(Object source) {
        return source instanceof InputStream || source instanceof InputStreamFileWrapper;
    }

    @Override
    public FileWrapper getFileWrapper(Object source,String name,String contentType,Long size) throws IOException {
        if (source instanceof InputStreamFileWrapper) {
            return updateFileWrapper((InputStreamFileWrapper) source,name,contentType,size);
        } else {
            InputStream inputStream = (InputStream) source;
            if (name == null) name = "";
            InputStreamFileWrapper wrapper = new InputStreamFileWrapper(inputStream,name,contentType,size);
            if (contentType == null) {
                wrapper.getInputStreamMaskReset(in -> wrapper.setContentType(contentTypeDetect.detect(in,wrapper.getName())));
            }
            return wrapper;
        }
    }

}
