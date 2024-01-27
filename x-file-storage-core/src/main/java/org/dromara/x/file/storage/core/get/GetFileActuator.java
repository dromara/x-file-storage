package org.dromara.x.file.storage.core.get;

import java.util.HashMap;
import java.util.List;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.GetFileAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 获取文件执行器
 */
public class GetFileActuator {
    private final FileStorageService fileStorageService;
    private final GetFilePretreatment pre;

    public GetFileActuator(GetFilePretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行获取文件
     */
    public RemoteFileInfo execute() {
        return execute(fileStorageService.getFileStorageVerify(pre.getPlatform()), fileStorageService.getAspectList());
    }

    /**
     * 执行获取文件
     */
    public RemoteFileInfo execute(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        Check.getFile(pre);
        return new GetFileAspectChain(aspectList, (_pre, _fileStorage) -> {
                    RemoteFileInfo info = _fileStorage.getFile(_pre);
                    if (info != null) {
                        if (info.getMetadata() == null) info.setMetadata(new HashMap<>());
                        if (info.getUserMetadata() == null) info.setUserMetadata(new HashMap<>());
                    }
                    return info;
                })
                .next(pre, fileStorage);
    }
}
