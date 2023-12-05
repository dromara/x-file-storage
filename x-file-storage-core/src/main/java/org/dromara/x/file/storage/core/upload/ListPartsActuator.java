package org.dromara.x.file.storage.core.upload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.ListPartsAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-列举已上传的分片执行器
 */
public class ListPartsActuator {
    private final FileStorageService fileStorageService;
    private final ListPartsPretreatment pre;

    public ListPartsActuator(ListPartsPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行列举已上传的分片
     */
    public List<FilePartInfo> execute() {
        FileInfo fileInfo = pre.getFileInfo();
        Check.listParts(fileInfo);

        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();

        return new ListPartsAspectChain(aspectList, (_pre, _fileStorage) -> _fileStorage.listParts(_pre))
                .next(pre, fileStorage);
    }
}
