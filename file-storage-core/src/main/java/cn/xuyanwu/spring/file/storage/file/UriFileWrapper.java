package cn.xuyanwu.spring.file.storage.file;

import cn.hutool.core.io.IoUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

/**
 * URI文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class UriFileWrapper implements FileWrapper {
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;


    public UriFileWrapper(InputStream inputStream,String name,String contentType,Long size) {
        this.name = name;
        this.contentType = contentType;
        this.inputStream = IoUtil.toMarkSupportStream(inputStream);
        this.size = size;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }
}
