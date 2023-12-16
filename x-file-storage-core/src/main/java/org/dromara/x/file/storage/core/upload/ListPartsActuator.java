package org.dromara.x.file.storage.core.upload;

import java.util.ArrayList;
import java.util.List;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.ListPartsAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MultipartUploadSupportInfo;

/**
 * 手动分片上传-列举已上传的分片执行器
 */
public class ListPartsActuator {
    private final FileStorageService fileStorageService;
    private final ListPartsPretreatment pre;

    public ListPartsActuator(ListPartsPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行列举已上传的分片
     */
    public FilePartInfoList execute() {
        return execute(
                fileStorageService.getFileStorageVerify(pre.getFileInfo().getPlatform()),
                fileStorageService.getAspectList());
    }

    /**
     * 执行列举已上传的分片
     */
    public FilePartInfoList execute(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        Check.listParts(pre.getFileInfo());
        return new ListPartsAspectChain(aspectList, (_pre, _fileStorage) -> {
                    MultipartUploadSupportInfo supportInfo = fileStorageService.isSupportMultipartUpload(_fileStorage);

                    // 获取对应存储平台每次获取的最大分片数，对象存储一般是 1000
                    Integer supportMaxParts = supportInfo.getListPartsSupportMaxParts();

                    // 如果要返回的最大分片数量为 null 或小于等于支持的最大分片数量，则直接调用，否则分多次调用后拼接成一个结果
                    if (supportMaxParts == null || _pre.getMaxParts() <= supportMaxParts) {
                        return _fileStorage.listParts(_pre);
                    } else {
                        FilePartInfoList list = new FilePartInfoList();
                        list.setFileInfo(_pre.getFileInfo());
                        list.setList(new ArrayList<>());
                        list.setMaxParts(_pre.getMaxParts());
                        list.setPartNumberMarker(_pre.getPartNumberMarker());

                        Integer residuePartNum = _pre.getMaxParts();
                        Integer partNumberMarker = _pre.getPartNumberMarker();
                        while (true) {
                            ListPartsPretreatment tempPre = new ListPartsPretreatment(_pre);
                            tempPre.setMaxParts(residuePartNum <= supportMaxParts ? residuePartNum : supportMaxParts);
                            tempPre.setPartNumberMarker(partNumberMarker);
                            FilePartInfoList tempList = _fileStorage.listParts(tempPre);
                            list.getList().addAll(tempList.getList());
                            residuePartNum = residuePartNum - supportMaxParts;
                            partNumberMarker = tempList.getNextPartNumberMarker();
                            if (residuePartNum <= 0 || !tempList.getIsTruncated()) {
                                list.setNextPartNumberMarker(tempList.getNextPartNumberMarker());
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
