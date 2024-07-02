package org.dromara.x.file.storage.core.presigned;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 生成预签名 URL 结果
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class GeneratePresignedUrlResult {
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * URL
     */
    private String url;
    /**
     * 访问 URL 时需要附带的请求头
     */
    private Map<String, String> headers;

    public GeneratePresignedUrlResult(String platform, String basePath, GeneratePresignedUrlPretreatment pre) {
        this.platform = platform;
        this.basePath = basePath;
        this.path = pre.getPath();
        this.filename = pre.getFilename();
    }
}
