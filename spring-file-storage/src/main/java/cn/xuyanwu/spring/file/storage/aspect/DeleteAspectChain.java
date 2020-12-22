package cn.xuyanwu.spring.file.storage.aspect;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 删除的切面调用链
 */
@Getter
@Setter
public class DeleteAspectChain {

    private DeleteAspectChainCallback callback;
    private List<FileStorageAspect> aspectList;
    private int index = -1;


    public DeleteAspectChain(List<FileStorageAspect> aspectList,DeleteAspectChainCallback callback) {
        this.aspectList = aspectList;
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileInfo fileInfo,FileStorage fileStorage,FileRecorder fileRecorder) {
        index++;
        if (aspectList.size() > index) {//还有下一个
            return aspectList.get(index).deleteAround(this,fileInfo,fileStorage,fileRecorder);
        } else {
            return callback.run(fileInfo,fileStorage,fileRecorder);
        }
    }
}
