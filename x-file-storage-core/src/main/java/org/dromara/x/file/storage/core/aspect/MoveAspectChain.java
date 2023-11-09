package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 移动的切面调用链
 */
@Getter
@Setter
public class MoveAspectChain {

    private MoveAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public MoveAspectChain(Iterable<FileStorageAspect> aspects, MoveAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(
            FileInfo srcFileInfo, MovePretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().moveAround(this, srcFileInfo, pre, fileStorage, fileRecorder);
        } else {
            return callback.run(srcFileInfo, pre, fileStorage, fileRecorder);
        }
    }
}
