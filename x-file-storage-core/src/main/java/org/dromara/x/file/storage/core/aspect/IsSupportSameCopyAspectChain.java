package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 是否支持同存储平台复制的切面调用链
 */
@Getter
@Setter
public class IsSupportSameCopyAspectChain {

    private IsSupportSameCopyAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportSameCopyAspectChain(
            Iterable<FileStorageAspect> aspects, IsSupportSameCopyAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().isSupportSameCopyAround(this, fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
