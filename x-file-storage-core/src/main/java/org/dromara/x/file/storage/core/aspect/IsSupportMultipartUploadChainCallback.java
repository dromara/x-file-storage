package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 是否支持手动分片上传的切面调用链结束回调
 */
public interface IsSupportMultipartUploadChainCallback {
    boolean run(FileStorage fileStorage);
}
