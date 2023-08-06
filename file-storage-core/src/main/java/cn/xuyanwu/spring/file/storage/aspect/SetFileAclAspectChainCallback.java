package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;

/**
 * 获取文件的访问控制列表调用链结束回调
 */
public interface SetFileAclAspectChainCallback {
    boolean run(FileInfo fileInfo,Object acl,FileStorage fileStorage);
}
