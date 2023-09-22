package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 下载缩略图切面调用链结束回调
 */
public interface DownloadThAspectChainCallback {
    void run(FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer);
}
