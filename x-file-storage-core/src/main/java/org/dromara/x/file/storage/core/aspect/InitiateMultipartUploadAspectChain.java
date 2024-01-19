package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;

/**
 * 手动分片上传-初始化的切面调用链
 */
@Getter
@Setter
public class InitiateMultipartUploadAspectChain {

    private InitiateMultipartUploadAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public InitiateMultipartUploadAspectChain(
            Iterable<FileStorageAspect> aspects, InitiateMultipartUploadAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(
            FileInfo fileInfo,
            InitiateMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().initiateMultipartUploadAround(this, fileInfo, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(fileInfo, pre, fileStorage, fileRecorder);
        }
    }
}
