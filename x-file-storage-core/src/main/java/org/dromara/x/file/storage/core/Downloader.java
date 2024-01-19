package org.dromara.x.file.storage.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.aspect.DownloadAspectChain;
import org.dromara.x.file.storage.core.aspect.DownloadThAspectChain;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.hash.HashCalculator;
import org.dromara.x.file.storage.core.hash.HashCalculatorManager;
import org.dromara.x.file.storage.core.hash.HashCalculatorSetter;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 下载器
 */
public class Downloader implements ProgressListenerSetter<Downloader>, HashCalculatorSetter<Downloader> {
    /**
     * 下载目标：文件
     */
    public static final int TARGET_FILE = 1;
    /**
     * 下载目标：缩略图文件
     */
    public static final int TARGET_TH_FILE = 2;

    private final FileStorage fileStorage;
    private final List<FileStorageAspect> aspectList;
    private final FileInfo fileInfo;
    private final Integer target;

    @Setter
    @Accessors(chain = true)
    private ProgressListener progressListener;
    /**
     * 哈希计算器管理器
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    private HashCalculatorManager hashCalculatorManager = new HashCalculatorManager();

    /**
     * 构造下载器
     *
     * @param target 下载目标：{@link Downloader#TARGET_FILE}下载文件，{@link Downloader#TARGET_TH_FILE}下载缩略图文件
     */
    public Downloader(FileInfo fileInfo, List<FileStorageAspect> aspectList, FileStorage fileStorage, Integer target) {
        this.fileStorage = fileStorage;
        this.aspectList = aspectList;
        this.fileInfo = fileInfo;
        this.target = target;
    }

    /**
     * 添加一个哈希计算器
     * @param hashCalculator 哈希计算器
     */
    @Override
    public Downloader setHashCalculator(HashCalculator hashCalculator) {
        hashCalculatorManager.setHashCalculator(hashCalculator);
        return this;
    }

    /**
     * 设置哈希计算器管理器（如果条件为 true）
     * @param flag 条件
     * @param hashCalculatorManager 哈希计算器管理器
     */
    public Downloader setHashCalculatorManager(boolean flag, HashCalculatorManager hashCalculatorManager) {
        if (flag) setHashCalculatorManager(hashCalculatorManager);
        return this;
    }

    /**
     * 获取 InputStream ，在此方法结束后会自动关闭 InputStream
     */
    public void inputStream(Consumer<InputStream> consumer) {
        if (target == TARGET_FILE) { // 下载文件
            new DownloadAspectChain(
                            aspectList,
                            (_fileInfo, _fileStorage, _consumer) -> _fileStorage.download(_fileInfo, _consumer))
                    .next(
                            fileInfo,
                            fileStorage,
                            in -> consumer.accept(new InputStreamPlus(
                                    in, progressListener, fileInfo.getSize(), hashCalculatorManager)));
        } else if (target == TARGET_TH_FILE) { // 下载缩略图文件
            new DownloadThAspectChain(
                            aspectList,
                            (_fileInfo, _fileStorage, _consumer) -> _fileStorage.downloadTh(_fileInfo, _consumer))
                    .next(
                            fileInfo,
                            fileStorage,
                            in -> consumer.accept(new InputStreamPlus(
                                    in, progressListener, fileInfo.getThSize(), hashCalculatorManager)));
        } else {
            throw new FileStorageRuntimeException("没找到对应的下载目标，请设置 target 参数！");
        }
    }

    /**
     * 下载 byte 数组
     */
    public byte[] bytes() {
        byte[][] bytes = new byte[1][];
        inputStream(in -> bytes[0] = IoUtil.readBytes(in));
        return bytes[0];
    }

    /**
     * 下载到指定文件
     */
    public void file(File file) {
        inputStream(in -> FileUtil.writeFromStream(in, file));
    }

    /**
     * 下载到指定文件
     */
    public void file(String filename) {
        inputStream(in -> FileUtil.writeFromStream(in, filename));
    }

    /**
     * 下载到指定输出流
     */
    public void outputStream(OutputStream out) {
        inputStream(in -> IoUtil.copy(in, out));
    }
}
