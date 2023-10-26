package org.dromara.x.file.storage.core.copy;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.constant.Constant.CopyMode;

/**
 * 复制预处理
 */
@Accessors(chain = true)
@Getter
@Setter
public class CopyPretreatment {
    private final FileStorageService fileStorageService;
    private final FileInfo fileInfo;
    /**
     * 复制模式
     */
    private CopyMode copyMode = CopyMode.AUTO;
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
    public CopyPretreatment(FileInfo fileInfo, FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        this.fileInfo = fileInfo;
        this.platform = fileInfo.getPlatform();
        this.path = fileInfo.getPath();
        this.filename = fileInfo.getFilename();
        this.thFilename = fileInfo.getThFilename();
    }

    /**
     * 设置复制模式
     */
    public CopyPretreatment setCopyMode(boolean flag, CopyMode copyMode) {
        if (flag) this.copyMode = copyMode;
        return this;
    }

    /**
     * 设置存储平台
     */
    public CopyPretreatment setPlatform(boolean flag, String platform) {
        if (flag) this.platform = platform;
        return this;
    }

    /**
     * 设置文件存储路径
     */
    public CopyPretreatment setPath(boolean flag, String path) {
        if (flag) this.path = path;
        return this;
    }

    /**
     * 设置文件名称
     */
    public CopyPretreatment setFilename(boolean flag, String filename) {
        if (flag) this.filename = filename;
        return this;
    }

    /**
     * 设置缩略图名称
     */
    public CopyPretreatment setThFilename(boolean flag, String thFilename) {
        if (flag) this.thFilename = thFilename;
        return this;
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public CopyPretreatment setProgressListener(Consumer<Long> progressListener) {
        return setProgressListener((progressSize, allSize) -> progressListener.accept(progressSize));
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public CopyPretreatment setProgressListener(boolean flag, Consumer<Long> progressListener) {
        if (flag) setProgressListener((progressSize, allSize) -> progressListener.accept(progressSize));
        return this;
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public CopyPretreatment setProgressListener(BiConsumer<Long, Long> progressListener) {
        return setProgressListener(new ProgressListener() {
            @Override
            public void start() {}

            @Override
            public void progress(long progressSize, Long allSize) {
                progressListener.accept(progressSize, allSize);
            }

            @Override
            public void finish() {}
        });
    }

    /**
     * 设置复制进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public CopyPretreatment setProgressListener(boolean flag, BiConsumer<Long, Long> progressListener) {
        if (flag) setProgressListener(progressListener);
        return this;
    }

    /**
     * 设置复制进度监听器
     */
    public CopyPretreatment setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    /**
     * 设置复制进度监听器
     */
    public CopyPretreatment setProgressListener(boolean flag, ProgressListener progressListener) {
        if (flag) this.progressListener = progressListener;
        return this;
    }

    /**
     * 设置不支持元数据时抛出异常
     */
    public CopyPretreatment setNotSupportMetadataThrowException(
            boolean flag, Boolean notSupportMetadataThrowException) {
        if (flag) this.notSupportMetadataThrowException = notSupportMetadataThrowException;
        return this;
    }

    /**
     * 设置不支持 ACL 时抛出异常
     */
    public CopyPretreatment setNotSupportAclThrowException(boolean flag, Boolean notSupportAclThrowException) {
        if (flag) this.notSupportAclThrowException = notSupportAclThrowException;
        return this;
    }

    /**
     * 复制文件，成功后返回新的 FileInfo
     */
    public FileInfo copy() {
        return new CopyActuator(this).execute();
    }
}
