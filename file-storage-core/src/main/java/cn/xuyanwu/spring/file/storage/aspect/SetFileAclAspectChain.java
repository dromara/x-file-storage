package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

/**
 * 获取文件的访问控制列表的切面调用链
 */
@Getter
@Setter
public class SetFileAclAspectChain {

    private SetFileAclAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public SetFileAclAspectChain(Iterable<FileStorageAspect> aspects,SetFileAclAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileInfo fileInfo,Object acl,FileStorage fileStorage) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().setFileAcl(this,fileInfo,acl,fileStorage);
        } else {
            return callback.run(fileInfo,acl,fileStorage);
        }
    }
}
