package org.dromara.x.file.storage.core.upload;

import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.UploadPartAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 手动分片上传-上传分片执行器
 */
public class UploadPartActuator {
    private final FileStorageService fileStorageService;
    private final UploadPartPretreatment pre;

    public UploadPartActuator(UploadPartPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行上传
     */
    public FilePartInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();
        Check.uploadPart(fileInfo);

        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();
        FileRecorder fileRecorder = fileStorageService.getFileRecorder();

        return new UploadPartAspectChain(aspectList, (_pre, _fileStorage, _fileRecorder) -> {
                    FilePartInfo filePartInfo = _fileStorage.uploadPart(_pre);
                    filePartInfo.setHashInfo(_pre.getHashCalculatorManager().getHashInfo());
                    _fileRecorder.saveFilePart(filePartInfo);
                    return filePartInfo;
                })
                .next(pre, fileStorage, fileRecorder);
    }
}
