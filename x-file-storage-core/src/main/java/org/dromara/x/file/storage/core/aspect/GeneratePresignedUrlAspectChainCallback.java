package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;

/**
 * 对文件生成可以签名访问的 URL 切面调用链结束回调
 */
public interface GeneratePresignedUrlAspectChainCallback {
    GeneratePresignedUrlResult run(GeneratePresignedUrlPretreatment pre, FileStorage fileStorage);
}
