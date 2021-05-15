package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;

/**
 * 删除切面调用链结束回调
 */
public interface DeleteAspectChainCallback {
    boolean run(FileInfo fileInfo,FileStorage fileStorage,FileRecorder fileRecorder);
}
