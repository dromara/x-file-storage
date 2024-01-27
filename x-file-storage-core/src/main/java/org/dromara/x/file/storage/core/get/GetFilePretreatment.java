package org.dromara.x.file.storage.core.get;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 获取文件预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class GetFilePretreatment {
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
     * 文件名
     */
    private String filename = "";
    /**
     * 执行获取文件
     */
    public RemoteFileInfo getFile() {
        return new GetFileActuator(this).execute();
    }

    /**
     * 执行获取文件，此方法仅限内部使用
     */
    public RemoteFileInfo getFile(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        return new GetFileActuator(this).execute(fileStorage, aspectList);
    }
}
