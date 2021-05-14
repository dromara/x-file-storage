package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * 下载缩略图的切面调用链
 */
@Getter
@Setter
public class DownloadThAspectChain {

    private DownloadThAspectChainCallback callback;
    private List<FileStorageAspect> aspectList;
    private int index = -1;


    public DownloadThAspectChain(List<FileStorageAspect> aspectList,DownloadThAspectChainCallback callback) {
        this.aspectList = aspectList;
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public void next(FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        index++;
        if (aspectList.size() > index) {//还有下一个
            aspectList.get(index).downloadThAround(this,fileInfo,fileStorage,consumer);
        } else {
            callback.run(fileInfo,fileStorage,consumer);
        }
    }
}
