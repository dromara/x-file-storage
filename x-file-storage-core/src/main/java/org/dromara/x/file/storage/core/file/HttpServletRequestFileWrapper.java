package org.dromara.x.file.storage.core.file;

import cn.hutool.core.io.IoUtil;
import java.io.InputStream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.file.MultipartFormDataReader.MultipartFormData;

/**
 * JavaxHttpServletRequest 文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class HttpServletRequestFileWrapper implements FileWrapper {
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;
    private MultipartFormData multipartFormData;

    public HttpServletRequestFileWrapper(
            InputStream inputStream, String name, String contentType, Long size, MultipartFormData multipartFormData) {
        this.name = name;
        this.contentType = contentType;
        this.inputStream = IoUtil.toMarkSupportStream(inputStream);
        this.size = size;
        this.multipartFormData = multipartFormData;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * 获取参数值
     */
    public String getParameter(String name) {
        return multipartFormData.getParameter(name);
    }

    /**
     * 获取多个参数值
     */
    public String[] getParameterValues(String name) {
        return multipartFormData.getParameterValues(name);
    }
}
