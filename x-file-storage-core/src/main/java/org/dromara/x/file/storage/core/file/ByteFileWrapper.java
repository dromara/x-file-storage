package org.dromara.x.file.storage.core.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public ByteFileWrapper(byte[] bytes, String name, String contentType, Long size) {
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
