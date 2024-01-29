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
     * 文件访问地址，仅用于兼容 FastDFS 的 URL 模式
     */
    private String url = "";

    /**
     * 如果条件为 true 则：设置文件存储服务类
     * @param flag 条件
     * @param fileStorageService 文件存储服务类
     * @return 获取文件预处理器
     */
    public GetFilePretreatment setFileStorageService(boolean flag, FileStorageService fileStorageService) {
        if (flag) setFileStorageService(fileStorageService);
        return this;
    }

    /**
     * 设置存储平台名称（如果条件为 true）
     * @param flag 条件
     * @param platform 存储平台名称
     * @return 获取文件预处理器
     */
    public GetFilePretreatment setPlatform(boolean flag, String platform) {
        if (flag) setPlatform(platform);
        return this;
    }

    /**
     * 设置路径，需要与上传时传入的路径保持一致（如果条件为 true）
     * @param flag 路径，需要与上传时传入的路径保持一致
     * @param path 文件访问地址
     * @return 获取文件预处理器
     */
    public GetFilePretreatment setPath(boolean flag, String path) {
        if (flag) setPath(path);
        return this;
    }

    /**
     * 设置文件名（如果条件为 true）
     * @param flag 条件
     * @param filename 文件名
     * @return 获取文件预处理器
     */
    public GetFilePretreatment setFilename(boolean flag, String filename) {
        if (flag) setFilename(filename);
        return this;
    }

    /**
     * 设置文件访问地址（如果条件为 true）
     * @param flag 条件
     * @param url 文件访问地址
     * @return 获取文件预处理器
     */
    public GetFilePretreatment setUrl(boolean flag, String url) {
        if (flag) setUrl(url);
        return this;
    }

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
