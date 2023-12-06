package org.dromara.x.file.storage.core.upload;

import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.AbortMultipartUploadAspectChain;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 手动分片上传-取消执行器
 */
public class AbortMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final AbortMultipartUploadPretreatment pre;

    public AbortMultipartUploadActuator(AbortMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行取消
     */
    public FileInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();
        Check.abortMultipartUpload(fileInfo);

        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        if (!fileStorageService.isSupportMultipartUpload(fileStorage)) {
            throw new FileStorageRuntimeException("手动分片上传-取消失败，当前存储平台不支持此功能");
        }
        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();
        FileRecorder fileRecorder = fileStorageService.getFileRecorder();

        return new AbortMultipartUploadAspectChain(aspectList, (_pre, _fileStorage, _fileRecorder) -> {
                    FileInfo _fileInfo = _pre.getFileInfo();
                    _fileStorage.abortMultipartUpload(_pre);
                    _fileRecorder.deleteFilePartByUploadId(_fileInfo.getUploadId());
                    _fileRecorder.delete(_fileInfo.getUrl());
                    return _fileInfo;
                })
                .next(pre, fileStorage, fileRecorder);
    }
}
