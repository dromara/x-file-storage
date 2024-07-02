package org.dromara.x.file.storage.core.aspect;

import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;

/**
 * 对文件生成可以签名访问的 URL 的切面调用链
 */
@Getter
@Setter
public class GeneratePresignedUrlAspectChain {

    private GeneratePresignedUrlAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public GeneratePresignedUrlAspectChain(
            Iterable<FileStorageAspect> aspects, GeneratePresignedUrlAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public GeneratePresignedUrlResult next(GeneratePresignedUrlPretreatment pre, FileStorage fileStorage) {
        if (aspectIterator.hasNext()) { // 还有下一个
            return aspectIterator.next().generatePresignedUrlAround(this, pre, fileStorage);
        } else {
            return callback.run(pre, fileStorage);
        }
    }
}
