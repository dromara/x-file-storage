package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;

/**
 * 手动分片上传-初始化切面调用链结束回调
 */
public interface InitiateMultipartUploadAspectChainCallback {
    FileInfo run(
            FileInfo fileInfo,
            InitiateMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder);
}
