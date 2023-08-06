package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

/**
 * 是否支持文件的访问控制列表 的切面调用链
 */
@Getter
@Setter
public class IsSupportAclAspectChain {

    private IsSupportAclAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public IsSupportAclAspectChain(Iterable<FileStorageAspect> aspects,IsSupportAclAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().isSupportAclAround(this,fileStorage);
        } else {
            return callback.run(fileStorage);
        }
    }
}
