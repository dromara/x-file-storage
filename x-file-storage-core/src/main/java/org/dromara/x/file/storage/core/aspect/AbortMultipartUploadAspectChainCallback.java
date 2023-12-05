package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.AbortMultipartUploadPretreatment;

/**
 * 手动分片上传-取消切面调用链结束回调
 */
public interface AbortMultipartUploadAspectChainCallback {
    FileInfo run(AbortMultipartUploadPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder);
}
