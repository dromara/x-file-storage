package org.dromara.x.file.storage.core.aspect;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * 下载缩略图的切面调用链
 */
@Getter
@Setter
public class DownloadThAspectChain {

    private DownloadThAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public DownloadThAspectChain(Iterable<FileStorageAspect> aspects,DownloadThAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public void next(FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        if (aspectIterator.hasNext()) {//还有下一个
            aspectIterator.next().downloadThAround(this,fileInfo,fileStorage,consumer);
        } else {
            callback.run(fileInfo,fileStorage,consumer);
        }
    }
}
