package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

/**
 * 是否支持对文件生成可以签名访问的 URL 的切面调用链
 */
@Getter
@Setter
public class IsSupportPresignedUrlAspectChain {

    private IsSupportPresignedUrlAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportPresignedUrlAspectChain(Iterable<FileStorageAspect> aspects,IsSupportPresignedUrlAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().isSupportPresignedUrlAround(this,fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
