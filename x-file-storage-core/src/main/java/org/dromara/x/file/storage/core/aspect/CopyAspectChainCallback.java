package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 复制切面调用链结束回调
 */
public interface CopyAspectChainCallback {
    FileInfo run(FileInfo srcFileInfo, CopyPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder);
}
