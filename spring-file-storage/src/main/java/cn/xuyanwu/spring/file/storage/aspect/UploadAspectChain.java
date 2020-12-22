package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 上传的切面调用链
 */
@Getter
@Setter
public class UploadAspectChain {

    private UploadAspectChainCallback callback;
    private List<FileStorageAspect> aspectList;
    private int index = -1;


    public UploadAspectChain(List<FileStorageAspect> aspectList,UploadAspectChainCallback callback) {
        this.aspectList = aspectList;
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public FileInfo next(FileInfo fileInfo,UploadPretreatment pre,FileStorage fileStorage,FileRecorder fileRecorder) {
        index++;
        if (aspectList.size() > index) {//还有下一个
            return aspectList.get(index).uploadAround(this,fileInfo,pre,fileStorage,fileRecorder);
        } else {
            return callback.run(fileInfo,pre,fileStorage,fileRecorder);
        }
    }
}
