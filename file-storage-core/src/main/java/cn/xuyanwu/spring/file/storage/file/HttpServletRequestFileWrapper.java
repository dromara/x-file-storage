package cn.xuyanwu.spring.file.storage.file;

import cn.hutool.core.io.IoUtil;
import cn.xuyanwu.spring.file.storage.file.MultipartFormDataReader.MultipartFormData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

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


    public HttpServletRequestFileWrapper(InputStream inputStream,String name,String contentType,Long size,MultipartFormData multipartFormData) {
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
