package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 获取文件切面调用链结束回调
 */
public interface GetFileAspectChainCallback {
    RemoteFileInfo run(GetFilePretreatment pre, FileStorage fileStorage);
}
