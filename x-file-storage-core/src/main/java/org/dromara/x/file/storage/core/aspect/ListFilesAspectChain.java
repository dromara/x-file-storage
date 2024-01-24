package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.get.FileFileInfoList;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 列举文件的切面调用链
 */
@Getter
@Setter
public class ListFilesAspectChain {

    private ListFilesAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public ListFilesAspectChain(Iterable<FileStorageAspect> aspects, ListFilesAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileFileInfoList next(ListFilesPretreatment pre, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().listFiles(this, pre, fileStorage);
        } else {
            return callback.run(pre, fileStorage);
        }
    }
}
