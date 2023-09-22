package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 获取文件的访问控制列表调用链结束回调
 */
public interface SetFileAclAspectChainCallback {
    boolean run(FileInfo fileInfo,Object acl,FileStorage fileStorage);
}
