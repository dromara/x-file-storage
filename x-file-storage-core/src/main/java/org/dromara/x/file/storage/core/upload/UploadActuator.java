package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.List;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.UploadAspectChain;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 上传执行器
 */
public class UploadActuator {
    private final FileStorageService fileStorageService;
    private final UploadPretreatment pre;

    /**
     * 通过旧的 UploadPretreatment 构造
     */
    @Deprecated
    public UploadActuator(org.dromara.x.file.storage.core.UploadPretreatment pre) {
        this(new UploadPretreatment(pre));
    }

    /**
     * 通过新的 UploadPretreatment 构造
     */
    public UploadActuator(UploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行上传
     */
    public FileInfo execute() {
        return execute(
                fileStorageService.getFileStorage(pre.getPlatform()),
                fileStorageService.getFileRecorder(),
                fileStorageService.getAspectList());
    }

    /**
     * 上传文件，成功返回文件信息，失败返回 null
     */
    public FileInfo execute(FileStorage fileStorage, FileRecorder fileRecorder, List<FileStorageAspect> aspectList) {
        if (fileStorage == null)
            throw new FileStorageRuntimeException(StrUtil.format("没有找到对应的存储平台！platform:{}", pre.getPlatform()));

        FileWrapper file = pre.getFileWrapper();
        if (file == null) throw new FileStorageRuntimeException("文件不允许为 null ！");
        if (pre.getPlatform() == null) throw new FileStorageRuntimeException("platform 不允许为 null ！");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(file.getSize());
        fileInfo.setOriginalFilename(file.getName());
        fileInfo.setExt(FileNameUtil.getSuffix(file.getName()));
        fileInfo.setObjectId(pre.getObjectId());
        fileInfo.setObjectType(pre.getObjectType());
        fileInfo.setPath(pre.getPath());
        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setMetadata(pre.getMetadata());
        fileInfo.setUserMetadata(pre.getUserMetadata());
        fileInfo.setThMetadata(pre.getThMetadata());
        fileInfo.setThUserMetadata(pre.getThUserMetadata());
        fileInfo.setAttr(pre.getAttr());
        fileInfo.setFileAcl(pre.getFileAcl());
        fileInfo.setThFileAcl(pre.getThFileAcl());
        if (StrUtil.isNotBlank(pre.getSaveFilename())) {
            fileInfo.setFilename(pre.getSaveFilename());
        } else {
            fileInfo.setFilename(
                    IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }
        fileInfo.setContentType(file.getContentType());

        byte[] thumbnailBytes = pre.getThumbnailBytes();
        if (thumbnailBytes != null) {
            fileInfo.setThSize((long) thumbnailBytes.length);
            if (StrUtil.isNotBlank(pre.getSaveThFilename())) {
                fileInfo.setThFilename(pre.getSaveThFilename() + pre.getThumbnailSuffix());
            } else {
                fileInfo.setThFilename(fileInfo.getFilename() + pre.getThumbnailSuffix());
            }
            if (StrUtil.isNotBlank(pre.getThContentType())) {
                fileInfo.setThContentType(pre.getThContentType());
            } else {
                fileInfo.setThContentType(
                        fileStorageService.getContentTypeDetect().detect(thumbnailBytes, fileInfo.getThFilename()));
            }
        }

        // 处理切面
        return new UploadAspectChain(aspectList, (_fileInfo, _pre, _fileStorage, _fileRecorder) -> {
                    // 真正开始保存
                    if (_fileStorage.save(_fileInfo, _pre)) {
                        _fileInfo.setHashInfo(_pre.getHashCalculatorManager().getHashInfo());
                        if (_fileRecorder.save(_fileInfo)) {
                            return _fileInfo;
                        }
                    }
                    return null;
                })
                .next(fileInfo, pre, fileStorage, fileRecorder);
    }
}
