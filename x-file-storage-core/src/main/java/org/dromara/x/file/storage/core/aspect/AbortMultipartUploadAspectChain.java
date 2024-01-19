package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.AbortMultipartUploadPretreatment;

/**
 * 手动分片上传-取消的切面调用链
 */
@Getter
@Setter
public class AbortMultipartUploadAspectChain {

    private AbortMultipartUploadAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public AbortMultipartUploadAspectChain(
            Iterable<FileStorageAspect> aspects, AbortMultipartUploadAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(AbortMultipartUploadPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().abortMultipartUploadAround(this, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(pre, fileStorage, fileRecorder);
        }
    }
}
