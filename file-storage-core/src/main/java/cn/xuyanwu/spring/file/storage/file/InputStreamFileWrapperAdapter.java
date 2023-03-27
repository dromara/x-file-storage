package cn.xuyanwu.spring.file.storage.file;

import cn.hutool.core.io.IoUtil;
import cn.xuyanwu.spring.file.storage.tika.ContentTypeDetect;
import cn.xuyanwu.spring.file.storage.util.Tools;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
            return handleSize(updateFileWrapper((InputStreamFileWrapper) source,name,contentType,size));
        } else {
            InputStream inputStream = (InputStream) source;
            if (name == null) name = "";
            InputStreamFileWrapper wrapper = new InputStreamFileWrapper(inputStream,name,contentType,size);
            if (contentType == null) {
                wrapper.getInputStreamMaskReset(in -> wrapper.setContentType(contentTypeDetect.detect(in,wrapper.getName())));
            }
            return handleSize(wrapper);
        }
    }

    /**
     * 处理文件 size
     */
    public FileWrapper handleSize(FileWrapper fileWrapper) throws IOException {
        if (fileWrapper.getSize() == null) {
            log.warn("构造 InputStreamFileWrapper 时未传入 size 参数，将通过读取全部字节方式获取 size ，这种方式将占用大量内存，如果明确知道此 InputStream 的长度，请传入 size 参数！");
            fileWrapper.setSize(fileWrapper.getInputStreamMaskResetReturn(Tools::getSize));
        }
        return fileWrapper;
    }


}
