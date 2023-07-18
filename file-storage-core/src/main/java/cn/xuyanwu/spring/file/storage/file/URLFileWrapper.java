package cn.xuyanwu.spring.file.storage.file;

import cn.hutool.core.io.IoUtil;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * URL文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class URLFileWrapper implements FileWrapper {
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;


    public URLFileWrapper(InputStream inputStream,String name,String contentType,Long size) {
        this.name = name;
        this.contentType = contentType;
        this.inputStream = IoUtil.toMarkSupportStream(inputStream);
        this.size = size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public void transferTo(File dest) {
        throw new FileStorageRuntimeException("ByteFile 不支持 transferTo 方法");
    }

    @Override
    public boolean supportTransfer() {
        return false;
    }

    @Override
    public void close() throws Exception {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
