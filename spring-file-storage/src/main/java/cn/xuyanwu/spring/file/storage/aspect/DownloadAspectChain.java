package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * 下载的切面调用链
 */
@Getter
@Setter
public class DownloadAspectChain {

    private DownloadAspectChainCallback callback;
    private List<FileStorageAspect> aspectList;
    private int index = -1;


    public DownloadAspectChain(List<FileStorageAspect> aspectList,DownloadAspectChainCallback callback) {
        this.aspectList = aspectList;
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public void next(FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        index++;
        if (aspectList.size() > index) {//还有下一个
            aspectList.get(index).downloadAround(this,fileInfo,fileStorage,consumer);
        } else {
            callback.run(fileInfo,fileStorage,consumer);
        }
    }
}
