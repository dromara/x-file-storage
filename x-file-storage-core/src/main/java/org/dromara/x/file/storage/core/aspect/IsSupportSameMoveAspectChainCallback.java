package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 是否支持同存储平台移动的切面调用链结束回调
 */
public interface IsSupportSameMoveAspectChainCallback {
    boolean run(FileStorage fileStorage);
}
