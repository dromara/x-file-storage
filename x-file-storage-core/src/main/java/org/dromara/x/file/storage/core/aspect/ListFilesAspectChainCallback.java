package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 列举文件切面调用链结束回调
 */
public interface ListFilesAspectChainCallback {
    ListFilesResult run(ListFilesPretreatment pre, FileStorage fileStorage);
}
