package org.dromara.x.file.storage.core.get;

import cn.hutool.core.map.MapProxy;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 远程文件信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class RemoteFileInfo {
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
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * 文件 ETag，仅部分存储平台支持
     */
    private String eTag;
    /**
     * Content-Disposition，仅部分存储平台支持
     */
    private String contentDisposition;
    /**
     * MIME 类型，仅部分存储平台支持
     */
    private String contentType;
    /**
     * MD5，仅部分存储平台支持
     */
    private String contentMd5;
    /**
     * 最后修改时间
     */
    private Date lastModified;
    /**
     * 文件元数据，仅部分存储平台支持
     */
    private Map<String, Object> metadata;
    /**
     * 文件用户元数据，仅部分存储平台支持
     */
    private Map<String, Object> userMetadata;
    /**
     * 原始数据
     */
    private Object original;

    /**
     * 获取短横命名风格且不区分大小写的文件元数据，以下方式都获得的值相同，put进入的值也会被覆盖<br>
     * get("ContentType")<br>
     * get("Content_Type")<br>
     * get("HelloWorld_test")<br>
     * get("Content-Type")<br>
     * get("contentType")<br>
     */
    public MapProxy getKebabCaseInsensitiveMetadata() {
        if (metadata == null) return null;
        return new MapProxy(new KebabCaseInsensitiveMap<>(metadata));
    }

    /**
     * 获取短横命名风格的文件用户元数据，以下方式都获得的值相同，put进入的值也会被覆盖<br>
     * get("ContentType")<br>
     * get("Content_Type")<br>
     * get("HelloWorld_test")<br>
     * get("Content-Type")<br>
     * get("contentType")<br>
     */
    public MapProxy getKebabCaseInsensitiveUserMetadata() {
        if (userMetadata == null) return null;
        return new MapProxy(new KebabCaseInsensitiveMap<>(userMetadata));
    }

    /**
     * 获取原始数据并转换为指定类型
     */
    public <T> T getOriginal(Class<T> clazz) {
        if (original == null) return null;
        if (clazz.isInstance(original)) {
            return Tools.cast(original);
        }
        return null;
    }

    public FTPFile getOriginalFtp() {
        return getOriginal(FTPFile.class);
    }

    public LsEntry getOriginalSftp() {
        return getOriginal(LsEntry.class);
    }
}
