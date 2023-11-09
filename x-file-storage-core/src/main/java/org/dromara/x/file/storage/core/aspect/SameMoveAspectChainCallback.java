package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 同存储平台移动切面调用链结束回调
 */
public interface SameMoveAspectChainCallback {
    FileInfo run(
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder);
}
