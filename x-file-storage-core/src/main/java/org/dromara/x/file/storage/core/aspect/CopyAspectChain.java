package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 复制的切面调用链
 */
@Getter
@Setter
public class CopyAspectChain {

    private CopyAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public CopyAspectChain(Iterable<FileStorageAspect> aspects, CopyAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(
            FileInfo srcFileInfo, CopyPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().copyAround(this, srcFileInfo, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(srcFileInfo, pre, fileStorage, fileRecorder);
        }
    }
}
