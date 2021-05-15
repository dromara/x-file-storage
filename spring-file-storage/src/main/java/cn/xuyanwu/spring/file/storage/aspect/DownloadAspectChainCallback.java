package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 下载切面调用链结束回调
 */
public interface DownloadAspectChainCallback {
    void run(FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer);
}
