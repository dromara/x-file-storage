package cn.xuyanwu.spring.file.storage.file;

import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 本地文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class LocalFileWrapper implements FileWrapper {
    private File file;
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;


    public LocalFileWrapper(File file,String name,String contentType,Long size) {
        this.file = file;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        }
        return inputStream;
    }

    @Override
    public void transferTo(File dest) {
        throw new FileStorageRuntimeException("LocalFile 不支持 transferTo 方法");
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
