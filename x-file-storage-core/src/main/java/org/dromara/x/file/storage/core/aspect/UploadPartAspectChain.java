package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.UploadPartPretreatment;

/**
 * 手动分片上传-上传分片的切面调用链
 */
@Getter
@Setter
public class UploadPartAspectChain {

    private UploadPartAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public UploadPartAspectChain(Iterable<FileStorageAspect> aspects, UploadPartAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FilePartInfo next(UploadPartPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().uploadPart(this, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(pre, fileStorage, fileRecorder);
        }
    }
}
