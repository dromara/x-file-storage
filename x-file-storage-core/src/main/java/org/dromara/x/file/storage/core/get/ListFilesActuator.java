package org.dromara.x.file.storage.core.get;

import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.ListFilesAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;

import java.util.ArrayList;
import java.util.List;

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
    public FileFileInfoList execute() {
        return execute(fileStorageService.getFileStorageVerify(pre.getPlatform()), fileStorageService.getAspectList());
    }

    /**
     * 执行列举文件
     */
    public FileFileInfoList execute(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        Check.listFiles(pre);
        return new ListFilesAspectChain(aspectList, (_pre, _fileStorage) -> {
                    ListFilesSupportInfo supportInfo = fileStorageService.isSupportListFiles(_fileStorage);

                    // 获取对应存储平台每次获取的最大分片数，对象存储一般是 1000
                    Integer supportMaxFiles = supportInfo.getListPartsSupportMaxParts();

                    // 如果要返回的最大分片数量为 null 或小于等于支持的最大分片数量，则直接调用，否则分多次调用后拼接成一个结果
                    if (supportMaxFiles == null || _pre.getMaxFiles() <= supportMaxFiles) {
                        return _fileStorage.listFiles(_pre);
                    } else {
                        FileFileInfoList list = new FileFileInfoList();
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
                            FileFileInfoList tempList = _fileStorage.listFiles(tempPre);
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
