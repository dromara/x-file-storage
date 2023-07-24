package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.platform.FileStorage;

/**
 * 是否支持文件的访问控制列表 切面调用链结束回调
 */
public interface IsSupportAclAspectChainCallback {
    boolean run(FileStorage fileStorage);
}
