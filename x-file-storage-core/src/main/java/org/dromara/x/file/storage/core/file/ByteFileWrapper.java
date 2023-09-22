package org.dromara.x.file.storage.core.file;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
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
}
