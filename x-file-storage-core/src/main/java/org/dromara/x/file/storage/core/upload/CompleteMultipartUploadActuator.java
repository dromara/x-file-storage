package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.io.IoUtil;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.hutool.core.util.StrUtil;
import org.dromara.x.file.storage.core.Downloader;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.CompleteMultipartUploadAspectChain;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.MultipartUploadSupportInfo;
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
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo.getPlatform());
        fileInfo.setUploadStatus(Constant.FileInfoUploadStatus.COMPLETE);
        CopyOnWriteArrayList<FileStorageAspect> aspectList = fileStorageService.getAspectList();
        FileRecorder fileRecorder = fileStorageService.getFileRecorder();
        ContentTypeDetect contentTypeDetect = fileStorageService.getContentTypeDetect();

        // 处理切面
        return new CompleteMultipartUploadAspectChain(
                        aspectList, (_pre, _fileStorage, _fileRecorder, _contentTypeDetect) -> {
                            FileInfo _fileInfo = _pre.getFileInfo();
                            MultipartUploadSupportInfo supportInfo =
                                    fileStorageService.isSupportMultipartUpload(_fileStorage);

                            // 如果未传入分片信息，则获取全部分片
                            if (_pre.getPartInfoList() == null && supportInfo.getIsSupportListParts()) {
                                FilePartInfoList partInfoList =
                                        fileStorageService.listParts(_fileInfo).listParts(_fileStorage, aspectList);
                                _pre.setPartInfoList(partInfoList.getList());
                            }

                            _fileStorage.completeMultipartUpload(_pre);
                            _fileRecorder.update(_fileInfo);
                            _fileRecorder.deleteFilePartByUploadId(_fileInfo.getUploadId());

                            // 文件上传完成，识别文件 ContentType
                            if (StrUtil.isNotBlank(_fileInfo.getContentType())) {
                                try {
                                    new Downloader(_fileInfo, aspectList, _fileStorage, Downloader.TARGET_FILE)
                                            .inputStream(in -> {
                                                try {
                                                    _fileInfo.setContentType(_contentTypeDetect.detect(
                                                            in, _fileInfo.getOriginalFilename()));
                                                    // 这里静默关闭流，防止出现 Premature end of Content-Length
                                                    // delimited message body 错误
                                                    IoUtil.close(in);
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                    _fileRecorder.update(_fileInfo);
                                } catch (Exception ignored) {
                                }
                            }
                            return _fileInfo;
                        })
                .next(pre, fileStorage, fileRecorder, contentTypeDetect);
    }
}
