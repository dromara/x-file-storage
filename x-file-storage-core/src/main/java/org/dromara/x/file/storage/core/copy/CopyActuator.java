package org.dromara.x.file.storage.core.copy;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import org.dromara.x.file.storage.core.Downloader;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.CopyAspectChain;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.SameCopyAspectChain;
import org.dromara.x.file.storage.core.constant.Constant.CopyMode;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 复制执行器
 */
public class CopyActuator {
    private final FileStorageService fileStorageService;
    private final FileInfo fileInfo;
    private final CopyPretreatment pre;

    public CopyActuator(CopyPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
        this.fileInfo = pre.getFileInfo();
    }

    /**
     * 复制文件，成功后返回新的 FileInfo
     */
    public FileInfo execute() {
        return execute(
                fileStorageService.getFileStorageVerify(fileInfo.getPlatform()),
                fileStorageService.getFileRecorder(),
                fileStorageService.getAspectList());
    }

    /**
     * 复制文件，成功后返回新的 FileInfo
     */
    public FileInfo execute(FileStorage fileStorage, FileRecorder fileRecorder, List<FileStorageAspect> aspectList) {
        if (fileInfo == null) throw new FileStorageRuntimeException("fileInfo 不能为 null");
        if (fileInfo.getPlatform() == null) throw new FileStorageRuntimeException("fileInfo 的 platform 不能为 null");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("fileInfo 的 path 不能为 null");
        if (StrUtil.isBlank(fileInfo.getFilename())) {
            throw new FileStorageRuntimeException("fileInfo 的 filename 不能为空");
        }
        if (StrUtil.isNotBlank(fileInfo.getThFilename()) && StrUtil.isBlank(pre.getThFilename())) {
            throw new FileStorageRuntimeException("目标缩略图文件名不能为空");
        }

        // 处理切面
        return new CopyAspectChain(aspectList, (_srcFileInfo, _pre, _fileStorage, _fileRecorder) -> {
                    // 真正开始复制
                    FileInfo destFileInfo;
                    if (isSameCopy(_srcFileInfo, _pre, _fileStorage)) {
                        destFileInfo = sameCopy(_srcFileInfo, _pre, _fileStorage, _fileRecorder, aspectList);
                    } else {
                        destFileInfo = crossCopy(_srcFileInfo, _pre, _fileStorage, _fileRecorder, aspectList);
                    }
                    return destFileInfo;
                })
                .next(fileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 判断是否使用同存储平台复制
     */
    protected boolean isSameCopy(FileInfo srcFileInfo, CopyPretreatment pre, FileStorage fileStorage) {
        CopyMode copyMode = pre.getCopyMode();
        if (copyMode == CopyMode.SAME) {
            if (!fileStorageService.isSupportSameCopy(fileStorage)) {
                throw new FileStorageRuntimeException("存储平台【" + fileStorage.getPlatform() + "】不支持同存储平台复制");
            }
            return true;
        } else if (copyMode == CopyMode.CROSS) {
            return false;
        } else {
            return srcFileInfo.getPlatform().equals(pre.getPlatform())
                    && fileStorageService.isSupportSameCopy(fileStorage);
        }
    }

    /**
     * 同存储平台复制
     */
    protected FileInfo sameCopy(
            FileInfo srcFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            List<FileStorageAspect> aspectList) {
        // 检查文件名是否与原始的相同
        if ((srcFileInfo.getPath() + srcFileInfo.getFilename()).equals(pre.getPath() + pre.getFilename())) {
            throw new FileStorageRuntimeException("源文件与目标文件路径相同");
        }
        // 检查缩略图文件名是否与原始的相同
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())
                && (srcFileInfo.getPath() + srcFileInfo.getThFilename()).equals(pre.getPath() + pre.getThFilename())) {
            throw new FileStorageRuntimeException("源缩略图文件与目标缩略图文件路径相同");
        }

        FileInfo destFileInfo = new FileInfo();
        destFileInfo.setSize(srcFileInfo.getSize());
        destFileInfo.setFilename(pre.getFilename());
        destFileInfo.setOriginalFilename(srcFileInfo.getOriginalFilename());
        destFileInfo.setBasePath(srcFileInfo.getBasePath());
        destFileInfo.setPath(pre.getPath());
        destFileInfo.setExt(FileNameUtil.extName(pre.getFilename()));
        destFileInfo.setContentType(srcFileInfo.getContentType());
        destFileInfo.setPlatform(pre.getPlatform());
        destFileInfo.setThFilename(pre.getThFilename());
        destFileInfo.setThSize(srcFileInfo.getThSize());
        destFileInfo.setThContentType(srcFileInfo.getThContentType());
        destFileInfo.setObjectId(srcFileInfo.getObjectId());
        destFileInfo.setObjectType(srcFileInfo.getObjectType());
        if (srcFileInfo.getMetadata() != null) {
            destFileInfo.setMetadata(new LinkedHashMap<>(srcFileInfo.getMetadata()));
        }
        if (srcFileInfo.getUserMetadata() != null) {
            destFileInfo.setUserMetadata(new LinkedHashMap<>(srcFileInfo.getUserMetadata()));
        }
        if (srcFileInfo.getThMetadata() != null) {
            destFileInfo.setThMetadata(new LinkedHashMap<>(srcFileInfo.getThMetadata()));
        }
        if (srcFileInfo.getThUserMetadata() != null) {
            destFileInfo.setThUserMetadata(new LinkedHashMap<>(srcFileInfo.getThUserMetadata()));
        }
        if (srcFileInfo.getAttr() != null) {
            destFileInfo.setAttr(new Dict(destFileInfo.getAttr()));
        }
        destFileInfo.setFileAcl(srcFileInfo.getFileAcl());
        destFileInfo.setThFileAcl(srcFileInfo.getThFileAcl());
        destFileInfo.setCreateTime(new Date());

        return new SameCopyAspectChain(aspectList, (_srcfileInfo, _destFileInfo, _pre, _fileStorage, _fileRecorder) -> {
                    _fileStorage.sameCopy(_srcfileInfo, _destFileInfo, _pre);
                    _fileRecorder.save(_destFileInfo);
                    return _destFileInfo;
                })
                .next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 跨存储平台复制，通过从下载并重新上传来实现
     */
    protected FileInfo crossCopy(
            FileInfo srcFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            List<FileStorageAspect> aspectList) {
        // 下载缩略图
        byte[] thBytes = StrUtil.isNotBlank(srcFileInfo.getThFilename())
                ? new Downloader(srcFileInfo, aspectList, fileStorage, Downloader.TARGET_TH_FILE).bytes()
                : null;

        final FileInfo[] destFileInfoArr = new FileInfo[1];
        new Downloader(srcFileInfo, aspectList, fileStorage, Downloader.TARGET_FILE).inputStream(in -> {
            String thumbnailSuffix = FileNameUtil.extName(pre.getThFilename());
            if (StrUtil.isNotBlank(thumbnailSuffix)) thumbnailSuffix = "." + thumbnailSuffix;

            destFileInfoArr[0] = fileStorageService
                    .of(in, srcFileInfo.getOriginalFilename(), srcFileInfo.getContentType(), srcFileInfo.getSize())
                    .setPlatform(pre.getPlatform())
                    .setPath(pre.getPath())
                    .setSaveFilename(pre.getFilename())
                    .setContentType(srcFileInfo.getContentType())
                    .setSaveThFilename(thBytes != null, FileNameUtil.mainName(pre.getThFilename()))
                    .setThumbnailSuffix(thBytes != null, thumbnailSuffix)
                    .thumbnailOf(thBytes != null, thBytes)
                    .setThContentType(srcFileInfo.getThContentType())
                    .setObjectType(srcFileInfo.getObjectType())
                    .setObjectId(srcFileInfo.getObjectId())
                    .setNotSupportAclThrowException(
                            pre.getNotSupportAclThrowException() != null, pre.getNotSupportAclThrowException())
                    .setFileAcl(srcFileInfo.getFileAcl() != null, srcFileInfo.getFileAcl())
                    .setThFileAcl(srcFileInfo.getThFileAcl() != null, srcFileInfo.getThFileAcl())
                    .setNotSupportMetadataThrowException(
                            pre.getNotSupportMetadataThrowException() != null,
                            pre.getNotSupportMetadataThrowException())
                    .putMetadataAll(srcFileInfo.getMetadata() != null, srcFileInfo.getMetadata())
                    .putThMetadataAll(srcFileInfo.getThMetadata() != null, srcFileInfo.getThMetadata())
                    .putUserMetadataAll(srcFileInfo.getMetadata() != null, srcFileInfo.getUserMetadata())
                    .putThUserMetadataAll(srcFileInfo.getThUserMetadata() != null, srcFileInfo.getThUserMetadata())
                    .setProgressListener(pre.getProgressListener())
                    .putAttrAll(srcFileInfo.getAttr() != null, srcFileInfo.getAttr())
                    .upload(fileStorageService.getFileStorageVerify(pre.getPlatform()), fileRecorder, aspectList);
        });
        return destFileInfoArr[0];
    }
}
