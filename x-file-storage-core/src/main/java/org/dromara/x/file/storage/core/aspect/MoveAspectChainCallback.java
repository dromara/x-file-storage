package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 移动切面调用链结束回调
 */
public interface MoveAspectChainCallback {
    FileInfo run(FileInfo srcFileInfo, MovePretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder);
}
