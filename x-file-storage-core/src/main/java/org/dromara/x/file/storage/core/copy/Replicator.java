package org.dromara.x.file.storage.core.copy;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 复制器
 */
@Accessors(chain = true)
@Getter
@Setter
public class Replicator {
    private FileStorageService fileStorageService;
    private FileInfo fileInfo;
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 文件存储路径
     */
    private String path;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 缩略图名称
     */
    private String thFilename;
    /**
     * 复制进度监听器
     */
    private ProgressListener progressListener;
    /**
     * 不支持元数据时抛出异常
     */
    private Boolean notSupportMetadataThrowException = true;

    /**
     * 不支持 ACL 时抛出异常
     */
    private Boolean notSupportAclThrowException = true;


    /**
     * 构造文件复制器
     */
    public Replicator(FileInfo fileInfo,FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        this.fileInfo = fileInfo;
        this.platform = fileInfo.getPlatform();
        this.path = fileInfo.getPath();
        this.filename = fileInfo.getFilename();
        this.thFilename = fileInfo.getThFilename();
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public Replicator setProgressListener(Consumer<Long> progressListener) {
        return setProgressListener((progressSize,allSize) -> progressListener.accept(progressSize));
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public Replicator setProgressListener(BiConsumer<Long, Long> progressListener) {
        return setProgressListener(new ProgressListener() {
            @Override
            public void start() {
            }

            @Override
            public void progress(long progressSize,Long allSize) {
                progressListener.accept(progressSize,allSize);
            }

            @Override
            public void finish() {
            }
        });
    }

    public Replicator setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    /**
     * 复制文件，成功后返回新的 FileInfo
     */
    public FileInfo copy() {
        if (fileInfo == null) throw new FileStorageRuntimeException("fileInfo 不能为 null");
        if (fileInfo.getPlatform() == null) throw new FileStorageRuntimeException("fileInfo 的 platform 不能为 null");
        if (fileInfo.getPath() == null) throw new FileStorageRuntimeException("fileInfo 的 path 不能为 null");
        if (StrUtil.isBlank(fileInfo.getFilename())) {
            throw new FileStorageRuntimeException("fileInfo 的 filename 不能为空");
        }
        if (StrUtil.isNotBlank(fileInfo.getThFilename()) && StrUtil.isBlank(thFilename)) {
            throw new FileStorageRuntimeException("目标缩略图文件名不能为空");
        }

        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        FileInfo destFileInfo;
        if (fileInfo.getPlatform().equals(platform) && fileStorage.isSupportCopy()) {
            destFileInfo = sameCopy();
            fileStorageService.getFileRecorder().save(destFileInfo);
        } else {
            destFileInfo = crossCopy();
        }
        return destFileInfo;
    }

    /**
     * 同平台复制
     */
    protected FileInfo sameCopy() {
        //检查文件名是否与原始的相同
        if ((fileInfo.getPath() + fileInfo.getFilename()).equals(path + filename)) {
            throw new FileStorageRuntimeException("源文件与目标文件路径相同");
        }
        //检查缩略图文件名是否与原始的相同
        if (StrUtil.isNotBlank(fileInfo.getThFilename()) && (fileInfo.getPath() + fileInfo.getThFilename()).equals(path + thFilename)) {
            throw new FileStorageRuntimeException("源缩略图文件与目标缩略图文件路径相同");
        }

        FileInfo destFileInfo = new FileInfo();
        destFileInfo.setId(null);
        destFileInfo.setUrl(null);
        destFileInfo.setSize(fileInfo.getSize());
        destFileInfo.setFilename(filename);
        destFileInfo.setOriginalFilename(fileInfo.getOriginalFilename());
        destFileInfo.setBasePath(fileInfo.getBasePath());
        destFileInfo.setPath(path);
        destFileInfo.setExt(FileNameUtil.extName(filename));
        destFileInfo.setContentType(fileInfo.getContentType());
        destFileInfo.setPlatform(platform);
        destFileInfo.setThUrl(null);
        destFileInfo.setThFilename(thFilename);
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

        fileStorageService.getFileStorageVerify(fileInfo.getPlatform()).copy(fileInfo,destFileInfo,progressListener);
        return destFileInfo;
    }

    /**
     * 跨平台复制，通过从下载并重新上传来实现
     */
    protected FileInfo crossCopy() {

        //下载缩略图
        byte[] thBytes = StrUtil.isNotBlank(fileInfo.getThFilename()) ? fileStorageService.downloadTh(fileInfo).bytes() : null;

        final FileInfo[] destFileInfo2 = new FileInfo[1];
        fileStorageService.download(fileInfo).inputStream(in -> {
            String thumbnailSuffix = FileNameUtil.extName(thFilename);
            if (StrUtil.isNotBlank(thumbnailSuffix)) thumbnailSuffix = "." + thumbnailSuffix;

            destFileInfo2[0] = fileStorageService.of(in,fileInfo.getOriginalFilename(),fileInfo.getContentType(),fileInfo.getSize())
                    .setPlatform(platform)
                    .setPath(path)
                    .setSaveFilename(filename)
                    .setContentType(fileInfo.getContentType())
                    .setSaveThFilename(thBytes != null,FileNameUtil.mainName(thFilename))
                    .setThumbnailSuffix(thBytes != null,thumbnailSuffix)
                    .thumbnailOf(thBytes != null,thBytes)
                    .setThContentType(fileInfo.getThContentType())
                    .setObjectType(fileInfo.getObjectType())
                    .setObjectId(fileInfo.getObjectId())
                    .setNotSupportAclThrowException(notSupportAclThrowException)
                    .setFileAcl(fileInfo.getFileAcl() != null,fileInfo.getFileAcl())
                    .setThFileAcl(fileInfo.getThFileAcl() != null,fileInfo.getThFileAcl())
                    .setNotSupportMetadataThrowException(notSupportMetadataThrowException)
                    .putMetadataAll(fileInfo.getMetadata() != null,fileInfo.getMetadata())
                    .putThMetadataAll(fileInfo.getThMetadata() != null,fileInfo.getThMetadata())
                    .putUserMetadataAll(fileInfo.getMetadata() != null,fileInfo.getUserMetadata())
                    .putThUserMetadataAll(fileInfo.getThUserMetadata() != null,fileInfo.getThUserMetadata())
                    .setProgressMonitor(progressListener)
                    .putAttrAll(fileInfo.getAttr() != null,fileInfo.getAttr())
                    .upload();
        });
        return destFileInfo2[0];
    }
}
