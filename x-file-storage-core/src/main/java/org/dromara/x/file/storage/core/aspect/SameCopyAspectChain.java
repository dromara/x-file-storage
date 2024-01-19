package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 同存储平台复制的切面调用链
 */
@Getter
@Setter
public class SameCopyAspectChain {

    private SameCopyAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public SameCopyAspectChain(Iterable<FileStorageAspect> aspects, SameCopyAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator
                    .next()
                    .sameCopyAround(this, srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
        }
    }
}
