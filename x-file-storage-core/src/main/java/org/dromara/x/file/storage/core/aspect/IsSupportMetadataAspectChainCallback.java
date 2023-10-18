package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 是否支持 Metadata 切面调用链结束回调
 */
public interface IsSupportMetadataAspectChainCallback {
    boolean run(FileStorage fileStorage);
}
