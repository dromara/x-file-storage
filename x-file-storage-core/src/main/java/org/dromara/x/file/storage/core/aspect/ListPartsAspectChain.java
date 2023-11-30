package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.ListPartsPretreatment;

/**
 * 手动分片上传-列举已上传的分片的切面调用链
 */
@Getter
@Setter
public class ListPartsAspectChain {

    private ListPartsAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public ListPartsAspectChain(Iterable<FileStorageAspect> aspects, ListPartsAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public List<FilePartInfo> next(ListPartsPretreatment pre, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().listParts(this, pre, fileStorage);
        } else {
            return callback.run(pre, fileStorage);
        }
    }
}
