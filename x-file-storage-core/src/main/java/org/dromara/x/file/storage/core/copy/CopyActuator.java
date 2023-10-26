package org.dromara.x.file.storage.core.copy;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.LinkedHashMap;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant.CopyMode;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 复制执行器
 */
public class CopyActuator {
    private final FileStorageService fileStorageService;
    private final FileStorage fileStorage;
    private final FileInfo fileInfo;
    private final CopyPretreatment pre;

    public CopyActuator(CopyPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
        this.fileInfo = pre.getFileInfo();
        this.fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
    }

    /**
     * 复制文件，成功后返回新的 FileInfo
     */
    public FileInfo execute() {
        if (fileInfo == null) throw new FileStorageRuntimeException("fileInfo 不能为 null");
        if (fileInfo.getPlatform() == null) throw new FileStorageRuntimeException("fileInfo 的 platform 不能为 null");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("fileInfo 的 path 不能为 null");
        if (StrUtil.isBlank(fileInfo.getFilename())) {
            throw new FileStorageRuntimeException("fileInfo 的 filename 不能为空");
        }
        if (StrUtil.isNotBlank(fileInfo.getThFilename()) && StrUtil.isBlank(pre.getThFilename())) {
            throw new FileStorageRuntimeException("目标缩略图文件名不能为空");
        }

        FileInfo destFileInfo;
        if (isSameCopy()) {
            destFileInfo = sameCopy();
            fileStorageService.getFileRecorder().save(destFileInfo);
        } else {
            destFileInfo = crossCopy();
        }
        return destFileInfo;
    }

    /**
     * 判断是否使用同平台复制
     */
    protected boolean isSameCopy() {
        CopyMode copyMode = pre.getCopyMode();
        if (copyMode == CopyMode.SAME) {
            return true;
        } else if (copyMode == CopyMode.CROSS) {
            return false;
        } else {
            return fileInfo.getPlatform().equals(pre.getPlatform()) && fileStorage.isSupportCopy();
        }
    }

    /**
     * 同平台复制
     */
    protected FileInfo sameCopy() {
        // 检查文件名是否与原始的相同
        if ((fileInfo.getPath() + fileInfo.getFilename()).equals(pre.getPath() + pre.getFilename())) {
            throw new FileStorageRuntimeException("源文件与目标文件路径相同");
        }
        // 检查缩略图文件名是否与原始的相同
        if (StrUtil.isNotBlank(fileInfo.getThFilename())
                && (fileInfo.getPath() + fileInfo.getThFilename()).equals(pre.getPath() + pre.getThFilename())) {
            throw new FileStorageRuntimeException("源缩略图文件与目标缩略图文件路径相同");
        }

        FileInfo destFileInfo = new FileInfo();
        destFileInfo.setId(null);
        destFileInfo.setUrl(null);
        destFileInfo.setSize(fileInfo.getSize());
        destFileInfo.setFilename(pre.getFilename());
        destFileInfo.setOriginalFilename(fileInfo.getOriginalFilename());
        destFileInfo.setBasePath(fileInfo.getBasePath());
        destFileInfo.setPath(pre.getPath());
        destFileInfo.setExt(FileNameUtil.extName(pre.getFilename()));
        destFileInfo.setContentType(fileInfo.getContentType());
        destFileInfo.setPlatform(pre.getPlatform());
        destFileInfo.setThUrl(null);
        destFileInfo.setThFilename(pre.getThFilename());
        destFileInfo.setThSize(fileInfo.getThSize());
        destFileInfo.setThContentType(fileInfo.getThContentType());
        destFileInfo.setObjectId(fileInfo.getObjectId());
        destFileInfo.setObjectType(fileInfo.getObjectType());
        if (fileInfo.getMetadata() != null) {
            destFileInfo.setMetadata(new LinkedHashMap<>(fileInfo.getMetadata()));
        }
        if (fileInfo.getUserMetadata() != null) {
            destFileInfo.setUserMetadata(new LinkedHashMap<>(fileInfo.getUserMetadata()));
        }
        if (fileInfo.getThMetadata() != null) {
            destFileInfo.setThMetadata(new LinkedHashMap<>(fileInfo.getThMetadata()));
        }
        if (fileInfo.getThUserMetadata() != null) {
            destFileInfo.setThUserMetadata(new LinkedHashMap<>(fileInfo.getThUserMetadata()));
        }
        if (fileInfo.getAttr() != null) {
            destFileInfo.setAttr(new Dict(destFileInfo.getAttr()));
        }
        destFileInfo.setFileAcl(fileInfo.getFileAcl());
        destFileInfo.setThFileAcl(fileInfo.getThFileAcl());
        destFileInfo.setCreateTime(new Date());

        fileStorage.copy(fileInfo, destFileInfo, pre.getProgressListener());
        return destFileInfo;
    }

    /**
     * 跨平台复制，通过从下载并重新上传来实现
     */
    protected FileInfo crossCopy() {
        // 下载缩略图
        byte[] thBytes = StrUtil.isNotBlank(fileInfo.getThFilename())
                ? fileStorageService.downloadTh(fileInfo).bytes()
                : null;

        final FileInfo[] destFileInfo2 = new FileInfo[1];
        fileStorageService.download(fileInfo).inputStream(in -> {
            String thumbnailSuffix = FileNameUtil.extName(pre.getThFilename());
            if (StrUtil.isNotBlank(thumbnailSuffix)) thumbnailSuffix = "." + thumbnailSuffix;

            destFileInfo2[0] = fileStorageService
                    .of(in, fileInfo.getOriginalFilename(), fileInfo.getContentType(), fileInfo.getSize())
                    .setPlatform(pre.getPlatform())
                    .setPath(pre.getPath())
                    .setSaveFilename(pre.getFilename())
                    .setContentType(fileInfo.getContentType())
                    .setSaveThFilename(thBytes != null, FileNameUtil.mainName(pre.getThFilename()))
                    .setThumbnailSuffix(thBytes != null, thumbnailSuffix)
                    .thumbnailOf(thBytes != null, thBytes)
                    .setThContentType(fileInfo.getThContentType())
                    .setObjectType(fileInfo.getObjectType())
                    .setObjectId(fileInfo.getObjectId())
                    .setNotSupportAclThrowException(
                            pre.getNotSupportAclThrowException() != null, pre.getNotSupportAclThrowException())
                    .setFileAcl(fileInfo.getFileAcl() != null, fileInfo.getFileAcl())
                    .setThFileAcl(fileInfo.getThFileAcl() != null, fileInfo.getThFileAcl())
                    .setNotSupportMetadataThrowException(
                            pre.getNotSupportMetadataThrowException() != null,
                            pre.getNotSupportMetadataThrowException())
                    .putMetadataAll(fileInfo.getMetadata() != null, fileInfo.getMetadata())
                    .putThMetadataAll(fileInfo.getThMetadata() != null, fileInfo.getThMetadata())
                    .putUserMetadataAll(fileInfo.getMetadata() != null, fileInfo.getUserMetadata())
                    .putThUserMetadataAll(fileInfo.getThUserMetadata() != null, fileInfo.getThUserMetadata())
                    .setProgressMonitor(pre.getProgressListener())
                    .putAttrAll(fileInfo.getAttr() != null, fileInfo.getAttr())
                    .upload();
        });
        return destFileInfo2[0];
    }
}
