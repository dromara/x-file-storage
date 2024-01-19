package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MultipartUploadSupportInfo;

/**
 * 是否支持手动分片上传的切面调用链
 */
@Getter
@Setter
public class IsSupportMultipartUploadAspectChain {

    private IsSupportMultipartUploadChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportMultipartUploadAspectChain(
            Iterable<FileStorageAspect> aspects, IsSupportMultipartUploadChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public MultipartUploadSupportInfo next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().isSupportMultipartUpload(this, fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
