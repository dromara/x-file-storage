package org.dromara.x.file.storage.core.get;

import java.util.ArrayList;
import java.util.List;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.ListFilesAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 列举文件执行器
 */
public class ListFilesActuator {
    private final FileStorageService fileStorageService;
    private final ListFilesPretreatment pre;

    public ListFilesActuator(ListFilesPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行列举文件
     */
    public ListFilesResult execute() {
        return execute(fileStorageService.getFileStorageVerify(pre.getPlatform()), fileStorageService.getAspectList());
    }

    /**
     * 执行列举文件
     */
    public ListFilesResult execute(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        Check.listFiles(pre);
        return new ListFilesAspectChain(aspectList, (_pre, _fileStorage) -> {
                    _pre = new ListFilesPretreatment(_pre);
                    ListFilesSupportInfo supportInfo = fileStorageService.isSupportListFiles(_fileStorage);

                    // 获取对应存储平台每次获取的最大文件数，对象存储一般是 1000
                    Integer supportMaxFiles = supportInfo.getSupportMaxFiles();

                    // 如果超出范围则直接取最大值
                    if (_pre.getMaxFiles() == null || _pre.getMaxFiles() < 1 || _pre.getMaxFiles() > supportMaxFiles) {
                        _pre.setMaxFiles(supportMaxFiles);
                    }

                    // 如果要返回的最大文件数量为 null 或小于等于支持的最大文件数量，则直接调用，否则分多次调用后拼接成一个结果
                    if (supportMaxFiles == null || _pre.getMaxFiles() <= supportMaxFiles) {
                        return _fileStorage.listFiles(_pre);
                    } else {
                        ListFilesResult list = new ListFilesResult();
                        list.setDirList(new ArrayList<>());
                        list.setFileList(new ArrayList<>());
                        list.setPlatform(_pre.getPlatform());
                        list.setPath(_pre.getPath());
                        list.setFilenamePrefix(_pre.getFilenamePrefix());
                        list.setMaxFiles(_pre.getMaxFiles());
                        list.setMarker(_pre.getMarker());

                        Integer residueFileNum = _pre.getMaxFiles();
                        String marker = _pre.getMarker();
                        while (true) {
                            ListFilesPretreatment tempPre = new ListFilesPretreatment(_pre);
                            tempPre.setMaxFiles(residueFileNum <= supportMaxFiles ? residueFileNum : supportMaxFiles);
                            tempPre.setMarker(marker);
                            ListFilesResult tempList = _fileStorage.listFiles(tempPre);
                            list.getFileList().addAll(tempList.getFileList());
                            list.getDirList().addAll(tempList.getDirList());
                            list.setBasePath(tempList.getBasePath());
                            residueFileNum = residueFileNum - supportMaxFiles;
                            marker = tempList.getNextMarker();
                            if (residueFileNum <= 0 || !tempList.getIsTruncated()) {
                                list.setNextMarker(tempList.getNextMarker());
                                list.setIsTruncated(tempList.getIsTruncated());
                                break;
                            }
                        }
                        return list;
                    }
                })
                .next(pre, fileStorage);
    }
}
