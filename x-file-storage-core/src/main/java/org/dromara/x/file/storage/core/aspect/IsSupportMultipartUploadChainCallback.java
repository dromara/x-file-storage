package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MultipartUploadSupportInfo;

/**
 * 是否支持手动分片上传的切面调用链结束回调
 */
public interface IsSupportMultipartUploadChainCallback {
    MultipartUploadSupportInfo run(FileStorage fileStorage);
}
