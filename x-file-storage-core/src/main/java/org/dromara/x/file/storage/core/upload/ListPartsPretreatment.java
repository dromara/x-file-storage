package org.dromara.x.file.storage.core.upload;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 手动分片上传-列举已上传的分片预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class ListPartsPretreatment {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;
    /**
     * 要列出的最大分片数量，正常情况下分片范围为 0~10000，这里默认最大值表示全部分片
     */
    private Integer maxParts = 10000;
    /**
     * 表示待列出分片的起始位置，只有分片号（partNumber）大于该参数的分片会被列出，默认从头开始
     */
    private Integer partNumberMarker = 0;

    /**
     * 如果条件为 true 则：设置文件存储服务类
     * @param flag 条件
     * @param fileStorageService 文件存储服务类
     * @return 手动分片上传-列举已上传的分片预处理器
     */
    public ListPartsPretreatment setFileStorageService(boolean flag, FileStorageService fileStorageService) {
        if (flag) setFileStorageService(fileStorageService);
        return this;
    }

    /**
     * 如果条件为 true 则：设置文件信息
     * @param flag 条件
     * @param fileInfo 文件信息
     * @return 手动分片上传-列举已上传的分片预处理器
     */
    public ListPartsPretreatment setFileInfo(boolean flag, FileInfo fileInfo) {
        if (flag) setFileInfo(fileInfo);
        return this;
    }

    /**
     * 如果条件为 true 则：设置要列出的最大分片数量，正常情况下分片范围为 0~10000，这里默认最大值表示全部分片
     * @param flag 条件
     * @param maxParts 要列出的最大分片数量，正常情况下分片范围为 0~10000，这里默认最大值表示全部分片
     * @return 手动分片上传-列举已上传的分片预处理器
     */
    public ListPartsPretreatment setMaxParts(boolean flag, Integer maxParts) {
        if (flag) setMaxParts(maxParts);
        return this;
    }

    /**
     * 如果条件为 true 则：设置表示待列出分片的起始位置，只有分片号（partNumber）大于该参数的分片会被列出，默认从头开始
     * @param flag 条件
     * @param partNumberMarker 表示待列出分片的起始位置，只有分片号（partNumber）大于该参数的分片会被列出，默认从头开始
     * @return 手动分片上传-列举已上传的分片预处理器
     */
    public ListPartsPretreatment setPartNumberMarker(boolean flag, Integer partNumberMarker) {
        if (flag) setPartNumberMarker(partNumberMarker);
        return this;
    }

    /**
     * 执行列举已上传的分片
     */
    public FilePartInfoList listParts() {
        return new ListPartsActuator(this).execute();
    }

    /**
     * 执行列举已上传的分片，此方法仅限内部使用
     */
    public FilePartInfoList listParts(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        return new ListPartsActuator(this).execute(fileStorage, aspectList);
    }

    /**
     * 通过已有的预处理来创建新的预处理器
     * @param pre 已有的预处理器
     */
    public ListPartsPretreatment(ListPartsPretreatment pre) {
        this.fileStorageService = pre.getFileStorageService();
        this.fileInfo = pre.getFileInfo();
        this.maxParts = pre.maxParts;
        this.partNumberMarker = pre.getPartNumberMarker();
    }
}
