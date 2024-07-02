package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 获取文件的切面调用链
 */
@Getter
@Setter
public class GetFileAspectChain {

    private GetFileAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public GetFileAspectChain(Iterable<FileStorageAspect> aspects, GetFileAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public RemoteFileInfo next(GetFilePretreatment pre, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().getFile(this, pre, fileStorage);
        } else {
            return callback.run(pre, fileStorage);
        }
    }
}
