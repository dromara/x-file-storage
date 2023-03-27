package cn.xuyanwu.spring.file.storage.spring.file;

import cn.xuyanwu.spring.file.storage.file.FileWrapper;
import cn.xuyanwu.spring.file.storage.file.FileWrapperAdapter;
import cn.xuyanwu.spring.file.storage.tika.ContentTypeDetect;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
            if (name == null) name = file.getName();
            if (contentType == null) contentType = file.getContentType();
            if (size == null) size = file.getSize();
            return new MultipartFileWrapper(file,name,contentType,size);
        }
    }
}
