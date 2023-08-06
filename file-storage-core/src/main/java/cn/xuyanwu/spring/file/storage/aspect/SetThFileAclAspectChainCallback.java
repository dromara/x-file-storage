package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;

/**
 * 设置缩略图文件的访问控制列表调用链结束回调
 */
public interface SetThFileAclAspectChainCallback {
    boolean run(FileInfo fileInfo,Object acl,FileStorage fileStorage);
}
