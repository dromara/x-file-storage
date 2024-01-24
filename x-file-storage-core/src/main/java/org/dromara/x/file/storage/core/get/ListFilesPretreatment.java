package org.dromara.x.file.storage.core.get;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.util.List;

/**
 * 列举文件预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class ListFilesPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 存储平台名称
     */
    private String platform;
    /**
     * 路径，需要与上传时传入的路径保持一致
     */
    private String path = "";
    /**
     * 文件名前缀
     */
    private String filenamePrefix = "";
    /**
     * 要列出的最大文件数量，这里默认最大值表示全部分片
     */
    private Integer maxFiles = Integer.MAX_VALUE;
    /**
     * 表示待列出文件的起始位置，从该标识符以后按文件名的字典顺序返回对象列表，默认从头开始
     */
    private String marker;

    /**
     * 执行列举文件
     */
    public FileFileInfoList listFiles() {
        return new ListFilesActuator(this).execute();
    }

    /**
     * 执行列举文件，此方法仅限内部使用
     */
    public FileFileInfoList listFiles(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        return new ListFilesActuator(this).execute(fileStorage, aspectList);
    }

    /**
     * 通过已有的预处理来创建新的预处理器
     * @param pre 已有的预处理器
     */
    public ListFilesPretreatment(ListFilesPretreatment pre) {
        this.fileStorageService = pre.getFileStorageService();
        this.platform = pre.getPlatform();
        this.path = pre.getPath();
        this.filenamePrefix = pre.getFilenamePrefix();
        this.maxFiles = pre.maxFiles;
        this.marker = pre.getMarker();
    }
}
