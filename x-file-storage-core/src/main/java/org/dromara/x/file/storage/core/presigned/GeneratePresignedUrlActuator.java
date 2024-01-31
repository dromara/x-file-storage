package org.dromara.x.file.storage.core.presigned;

import java.util.List;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.GeneratePresignedUrlAspectChain;
import org.dromara.x.file.storage.core.platform.FileStorage;

/**
 * 生成预签名 URL 执行器
 */
public class GeneratePresignedUrlActuator {
    private final FileStorageService fileStorageService;
    private final GeneratePresignedUrlPretreatment pre;

    public GeneratePresignedUrlActuator(GeneratePresignedUrlPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行生成预签名 URL
     */
    public GeneratePresignedUrlResult execute() {
        return execute(fileStorageService.getFileStorageVerify(pre.getPlatform()), fileStorageService.getAspectList());
    }

    /**
     * 执行生成预签名 URL
     */
    public GeneratePresignedUrlResult execute(FileStorage fileStorage, List<FileStorageAspect> aspectList) {
        //        Check.getFile(pre);
        return new GeneratePresignedUrlAspectChain(
                        aspectList, (_pre, _fileStorage) -> _fileStorage.generatePresignedUrl(_pre))
                .next(pre, fileStorage);
    }
}
