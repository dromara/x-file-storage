package org.dromara.x.file.storage.core.presigned;

import java.util.HashMap;
import java.util.List;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.aspect.GeneratePresignedUrlAspectChain;
import org.dromara.x.file.storage.core.exception.Check;
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
        Check.generatePresignedUrl(pre);
        return new GeneratePresignedUrlAspectChain(aspectList, (_pre, _fileStorage) -> {
                    GeneratePresignedUrlResult result = _fileStorage.generatePresignedUrl(_pre);
                    if (result.getHeaders() == null) result.setHeaders(new HashMap<>());
                    return result;
                })
                .next(pre, fileStorage);
    }
}
