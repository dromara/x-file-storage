package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.get.FileFileInfoList;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;

/**
 * 列举文件切面调用链结束回调
 */
public interface ListFilesAspectChainCallback {
    FileFileInfoList run(ListFilesPretreatment pre, FileStorage fileStorage);
}
