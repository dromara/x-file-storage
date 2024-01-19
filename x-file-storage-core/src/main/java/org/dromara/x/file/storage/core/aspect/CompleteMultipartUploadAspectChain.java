package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.CompleteMultipartUploadPretreatment;

/**
 * 手动分片上传-完成的切面调用链
 */
@Getter
@Setter
public class CompleteMultipartUploadAspectChain {

    private CompleteMultipartUploadAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public CompleteMultipartUploadAspectChain(
            Iterable<FileStorageAspect> aspects, CompleteMultipartUploadAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(
            CompleteMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            ContentTypeDetect contentTypeDetect) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator
                    .next()
                    .completeMultipartUploadAround(this, pre, fileStorage, fileRecorder, contentTypeDetect);
        } else {
            return callback.run(pre, fileStorage, fileRecorder, contentTypeDetect);
        }
    }
}
