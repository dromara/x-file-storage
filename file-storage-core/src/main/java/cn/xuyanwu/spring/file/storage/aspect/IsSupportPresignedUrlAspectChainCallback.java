package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.platform.FileStorage;

/**
 * 是否支持对文件生成可以签名访问的 URL 切面调用链结束回调
 */
public interface IsSupportPresignedUrlAspectChainCallback {
    boolean run(FileStorage fileStorage);
}
