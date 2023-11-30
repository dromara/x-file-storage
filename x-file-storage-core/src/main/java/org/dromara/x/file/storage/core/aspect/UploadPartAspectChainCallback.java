package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.UploadPartPretreatment;

/**
 * 手动分片上传-上传分片切面调用链结束回调
 */
public interface UploadPartAspectChainCallback {
    FilePartInfo run(UploadPartPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder);
}
