package org.dromara.x.file.storage.core.aspect;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.util.Date;
import java.util.Iterator;

/**
 * 对缩略图文件生成可以签名访问的 URL 的切面调用链
 */
@Getter
@Setter
public class GenerateThPresignedUrlAspectChain {

    private GenerateThPresignedUrlAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public GenerateThPresignedUrlAspectChain(Iterable<FileStorageAspect> aspects,GenerateThPresignedUrlAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public String next(FileInfo fileInfo,Date expiration,FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().generateThPresignedUrlAround(this,fileInfo,expiration,fileStorage);
        } else {
            return callback.run(fileInfo,expiration,fileStorage);
        }
    }
}
