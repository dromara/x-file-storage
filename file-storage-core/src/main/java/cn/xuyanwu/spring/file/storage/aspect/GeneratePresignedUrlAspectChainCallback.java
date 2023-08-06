package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;

import java.util.Date;

/**
 * 对文件生成可以签名访问的 URL 切面调用链结束回调
 */
public interface GeneratePresignedUrlAspectChainCallback {
    String run(FileInfo fileInfo,Date expiration,FileStorage fileStorage);
}
