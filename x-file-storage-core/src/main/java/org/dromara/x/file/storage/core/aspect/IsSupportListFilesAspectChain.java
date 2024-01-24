package org.dromara.x.file.storage.core.aspect;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.get.ListFilesSupportInfo;

import java.util.Iterator;

/**
 * 是否支持手动分片上传的切面调用链
 */
@Getter
@Setter
public class IsSupportListFilesAspectChain {

    private IsSupportListFilesChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportListFilesAspectChain(
            Iterable<FileStorageAspect> aspects, IsSupportListFilesChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public ListFilesSupportInfo next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().isSupportListFiles(this, fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
