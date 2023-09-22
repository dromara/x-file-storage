package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.util.Date;

/**
 * 对缩略图文件生成可以签名访问的 URL 切面调用链结束回调
 */
public interface GenerateThPresignedUrlAspectChainCallback {
    String run(FileInfo fileInfo,Date expiration,FileStorage fileStorage);
}
