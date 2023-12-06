package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.InitiateMultipartUploadAspectChain;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 手动分片上传-初始化执行器
 */
public class InitiateMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final InitiateMultipartUploadPretreatment pre;

    public InitiateMultipartUploadActuator(InitiateMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行初始化
     */
    public FileInfo execute() {
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(pre.getPlatform());
        if (!fileStorageService.isSupportMultipartUpload(fileStorage)) {
            throw new FileStorageRuntimeException("手动分片上传-初始化失败，当前存储平台不支持此功能");
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(pre.getSize());
        fileInfo.setOriginalFilename(pre.getOriginalFilename());
        fileInfo.setExt(FileNameUtil.getSuffix(pre.getOriginalFilename()));
        fileInfo.setObjectId(pre.getObjectId());
        fileInfo.setObjectType(pre.getObjectType());
        fileInfo.setPath(pre.getPath());
        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setMetadata(pre.getMetadata());
        fileInfo.setUserMetadata(pre.getUserMetadata());
        fileInfo.setAttr(pre.getAttr());
        fileInfo.setFileAcl(pre.getFileAcl());
        fileInfo.setUploadStatus(Constant.FileInfoUploadStatus.INITIATE);

        if (StrUtil.isNotBlank(pre.getSaveFilename())) {
            fileInfo.setFilename(pre.getSaveFilename());
        } else {
            fileInfo.setFilename(
                    IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }
        fileInfo.setContentType(pre.getContentType());

        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();
        FileRecorder fileRecorder = fileStorageService.getFileRecorder();

        // 处理切面
        return new InitiateMultipartUploadAspectChain(aspectList, (_fileInfo, _pre, _fileStorage, _fileRecorder) -> {
                    // 真正开始保存
                    _fileStorage.initiateMultipartUpload(_fileInfo, _pre);
                    try {
                        if (!_fileRecorder.save(_fileInfo)) {
                            throw new RuntimeException("文件记录保存失败");
                        }
                    } catch (Exception e) {
                        throw ExceptionFactory.initiateMultipartUploadRecorderSave(
                                _fileInfo, _fileStorage.getPlatform(), e);
                    }
                    return _fileInfo;
                })
                .next(fileInfo, pre, fileStorage, fileRecorder);
    }
}
