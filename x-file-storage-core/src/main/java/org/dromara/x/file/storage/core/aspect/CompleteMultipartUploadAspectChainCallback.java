package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.CompleteMultipartUploadPretreatment;

/**
 * 手动分片上传-完成切面调用链结束回调
 */
public interface CompleteMultipartUploadAspectChainCallback {
    FileInfo run(
            CompleteMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            ContentTypeDetect contentTypeDetect);
}
