package org.dromara.x.file.storage.core.upload;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.Downloader;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.CompleteMultipartUploadAspectChain;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;

/**
 * 手动分片上传-完成执行器
 */
public class CompleteMultipartUploadActuator {
    private final FileStorageService fileStorageService;
    private final CompleteMultipartUploadPretreatment pre;

    public CompleteMultipartUploadActuator(CompleteMultipartUploadPretreatment pre) {
        this.pre = pre;
        this.fileStorageService = pre.getFileStorageService();
    }

    /**
     * 执行完成
     */
    public FileInfo execute() {
        FileInfo fileInfo = pre.getFileInfo();
        Check.completeMultipartUpload(fileInfo);

        fileInfo.setUploadStatus(Constant.FileInfoUploadStatus.COMPLETE);
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());

        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();
        FileRecorder fileRecorder = fileStorageService.getFileRecorder();
        ContentTypeDetect contentTypeDetect = fileStorageService.getContentTypeDetect();

        // 处理切面
        return new CompleteMultipartUploadAspectChain(
                        aspectList, (_pre, _fileStorage, _fileRecorder, _contentTypeDetect) -> {
                            FileInfo _fileInfo = _pre.getFileInfo();
                            _fileStorage.completeMultipartUpload(_pre);
                            _fileRecorder.update(_fileInfo);
                            _fileRecorder.deleteFilePartByUploadId(_fileInfo.getUploadId());

                            // 文件上传完成，识别文件 ContentType
                            if (_fileInfo.getContentType() == null) {
                                new Downloader(_fileInfo, aspectList, _fileStorage, Downloader.TARGET_FILE)
                                        .inputStream(in -> {
                                            try {
                                                _fileInfo.setContentType(
                                                        _contentTypeDetect.detect(in, _fileInfo.getOriginalFilename()));
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });

                                _fileRecorder.update(_fileInfo);
                            }
                            return _fileInfo;
                        })
                .next(pre, fileStorage, fileRecorder, contentTypeDetect);
    }
}
