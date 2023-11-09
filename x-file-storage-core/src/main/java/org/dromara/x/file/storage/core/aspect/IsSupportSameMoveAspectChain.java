package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 是否支持同存储平台移动的切面调用链
 */
@Getter
@Setter
public class IsSupportSameMoveAspectChain {

    private IsSupportSameMoveAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportSameMoveAspectChain(
            Iterable<FileStorageAspect> aspects, IsSupportSameMoveAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().isSupportSameMoveAround(this, fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
