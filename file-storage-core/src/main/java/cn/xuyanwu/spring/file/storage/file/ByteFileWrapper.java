package cn.xuyanwu.spring.file.storage.file;

import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * byte[] 文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class ByteFileWrapper implements FileWrapper {
    private byte[] bytes;
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;


    public ByteFileWrapper(byte[] bytes,String name,String contentType,Long size) {
        this.bytes = bytes;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new ByteArrayInputStream(bytes);
        }
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
