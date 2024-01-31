package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.*;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.LocalPlusConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.upload.*;

/**
 * 本地文件存储升级版
 */
@Getter
@Setter
@NoArgsConstructor
public class LocalPlusFileStorage implements FileStorage {
    private String basePath;
    private String storagePath;
    private String platform;
    private String domain;

    public LocalPlusFileStorage(LocalPlusConfig config) {
        platform = config.getPlatform();
        basePath = config.getBasePath();
        domain = config.getDomain();
        storagePath = config.getStoragePath();
    }

    /**
     * 获取本地绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        try {
            File newFile = FileUtil.touch(getAbsolutePath(newFileKey));
            FileWrapper fileWrapper = pre.getFileWrapper();
            if (fileWrapper.supportTransfer()) { // 移动文件，速度较快
                ProgressListener.quickStart(pre.getProgressListener(), fileWrapper.getSize());
                fileWrapper.transferTo(newFile);
                ProgressListener.quickFinish(pre.getProgressListener(), fileWrapper.getSize());
                if (fileInfo.getSize() == null) fileInfo.setSize(newFile.length());
            } else { // 通过输入流写入文件
                InputStreamPlus in = pre.getInputStreamPlus();
                FileUtil.writeFromStream(in, newFile);
                if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            }

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                FileUtil.writeBytes(thumbnailBytes, getAbsolutePath(newThFileKey));
            }
            return true;
        } catch (IOException e) {
            try {
                FileUtil.del(getAbsolutePath(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll().setListPartsSupportMaxParts(10000);
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);
        try {
            String uploadId = IdUtil.objectId();
            String parent = FileUtil.file(getAbsolutePath(newFileKey)).getParent();
            FileUtil.mkdir(FileUtil.file(parent, uploadId));
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        pre.setHashCalculatorMd5();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            String parent = FileUtil.file(getAbsolutePath(newFileKey)).getParent();
            File dir = FileUtil.file(parent, fileInfo.getUploadId());
            File part = FileUtil.file(dir, String.valueOf(pre.getPartNumber()));
            FileUtil.writeFromStream(in, part);

            String etag = pre.getHashCalculatorManager().getHashInfo().getMd5();
            LocalPartInfo partInfo =
                    new LocalPartInfo(pre.getPartNumber(), etag, part.length(), new Date(part.lastModified()));
            FileUtil.appendUtf8String(partInfo.toIndexString() + "\n", FileUtil.file(dir, "index"));

            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            // etag 应该是分片的 MD5，这里暂时用随机 ID 代替，等流式计算 hash 功能好了再替换
            filePartInfo.setETag(etag);
            filePartInfo.setPartNumber(pre.getPartNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        try {
            File newFile = FileUtil.file(getAbsolutePath(newFileKey));
            String parent = newFile.getParent();
            File dir = FileUtil.file(parent, fileInfo.getUploadId());

            List<FilePartInfo> partInfoList = pre.getPartInfoList().stream()
                    .sorted(Comparator.comparingInt(FilePartInfo::getPartNumber))
                    .collect(Collectors.toList());

            // 合并文件
            Long fileSize = fileInfo.getSize();
            final long[] allProgressSize = {0};
            ProgressListener progressListener = pre.getProgressListener();
            ProgressListener.quickStart(progressListener, fileSize);
            try (BufferedOutputStream out = FileUtil.getOutputStream(newFile)) {
                for (FilePartInfo partInfo : partInfoList) {
                    File partFile = FileUtil.file(dir, String.valueOf(partInfo.getPartNumber()));
                    if (progressListener == null) {
                        FileUtil.writeToStream(partFile, out);
                    } else {
                        try (InputStream in = FileUtil.getInputStream(partFile)) {
                            IoUtil.copy(in, out, IoUtil.DEFAULT_BUFFER_SIZE, new StreamProgress() {
                                @Override
                                public void start() {}

                                @Override
                                public void progress(long total, long progressSize) {
                                    ProgressListener.quickProgress(
                                            progressListener, allProgressSize[0] + progressSize, fileSize);
                                }

                                @Override
                                public void finish() {
                                    allProgressSize[0] += partFile.length();
                                }
                            });
                        }
                    }
                }
            }
            ProgressListener.quickFinish(progressListener);
            if (fileSize == null) fileInfo.setSize(newFile.length());

            FileUtil.del(dir);
        } catch (Exception e) {
            try {
                FileUtil.del(getAbsolutePath(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        try {
            File newFile = FileUtil.file(getAbsolutePath(newFileKey));
            String parent = newFile.getParent();
            File dir = FileUtil.file(parent, fileInfo.getUploadId());
            FileUtil.del(dir);
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        try {
            String parent = FileUtil.file(getAbsolutePath(newFileKey)).getParent();
            File dir = FileUtil.file(parent, fileInfo.getUploadId());
            List<String> partTextInfoList = FileUtil.readUtf8Lines(FileUtil.file(dir, "index"));

            List<LocalPartInfo> localPartInfoList = partTextInfoList.stream()
                    .map(LocalPartInfo::new)
                    .filter(p -> p.getPartNumber() > pre.getPartNumberMarker())
                    .sorted(Comparator.comparingInt(LocalPartInfo::getPartNumber))
                    .collect(Collectors.toList());

            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setMaxParts(pre.getMaxParts());
            list.setPartNumberMarker(pre.getPartNumberMarker());

            if (localPartInfoList.size() > pre.getMaxParts()) {
                list.setIsTruncated(true);
                localPartInfoList = localPartInfoList.subList(0, pre.getMaxParts());
                list.setNextPartNumberMarker(
                        localPartInfoList.get(localPartInfoList.size() - 1).getPartNumber());
            } else {
                list.setIsTruncated(false);
            }

            list.setList(localPartInfoList.stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.getEtag());
                        filePartInfo.setPartNumber(p.getPartNumber());
                        filePartInfo.setPartSize(p.getSize());
                        filePartInfo.setLastModified(p.getLastModified());
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));

            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listParts(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll().setSupportMaxFiles(Integer.MAX_VALUE);
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        try {
            String path = getAbsolutePath(basePath + pre.getPath());
            List<File> fileList = Arrays.stream(FileUtil.isDirectory(path) ? FileUtil.ls(path) : new File[0])
                    .filter(f -> f.isFile() || f.isDirectory())
                    .collect(Collectors.toList());
            ListFilesMatchResult<File> matchResult = listFilesMatch(fileList, File::getName, pre, false);
            ListFilesResult list = new ListFilesResult();
            list.setDirList(matchResult.getList().stream()
                    .filter(File::isDirectory)
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(item.getName());
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(matchResult.getList().stream()
                    .filter(File::isFile)
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(item.getName());
                        info.setSize(item.length());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setLastModified(new Date(item.lastModified()));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(pre.getMaxFiles());
            list.setIsTruncated(matchResult.getIsTruncated());
            list.setMarker(pre.getMarker());
            list.setNextMarker(matchResult.getNextMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        try {
            File file = new File(getAbsolutePath(fileKey));
            if (!file.exists()) return null;
            if (!file.isFile()) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(file.getName());
            info.setUrl(domain + fileKey);
            info.setSize(file.length());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setLastModified(new Date(file.lastModified()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                FileUtil.del(getAbsolutePath(getThFileKey(fileInfo)));
            }
            return FileUtil.del(getAbsolutePath(getFileKey(fileInfo)));
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return new File(getAbsolutePath(getFileKey(fileInfo))).exists();
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (InputStream in = FileUtil.getInputStream(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
        }

        // 复制缩略图文件
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                File srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.copyFile(srcThFile, destThFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                try {
                    FileUtil.del(destThFile);
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        File destFile = null;
        try {
            destFile = FileUtil.touch(getAbsolutePath(destFileKey));
            if (pre.getProgressListener() == null) {
                FileUtil.copyFile(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                InputStreamPlus in = new InputStreamPlus(
                        FileUtil.getInputStream(srcFile), pre.getProgressListener(), srcFile.length());
                FileUtil.writeFromStream(in, destFile);
            }
        } catch (Exception e) {
            try {
                FileUtil.del(destThFile);
            } catch (Exception ignored) {
            }
            try {
                FileUtil.del(destFile);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        File srcFile = new File(getAbsolutePath(getFileKey(srcFileInfo)));
        if (!srcFile.exists()) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, null);
        }

        // 移动缩略图文件
        File srcThFile = null;
        File destThFile = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                srcThFile = new File(getAbsolutePath(getThFileKey(srcFileInfo)));
                destThFile = FileUtil.touch(getAbsolutePath(destThFileKey));
                FileUtil.move(srcThFile, destThFile, true);
            } catch (Exception e) {
                try {
                    FileUtil.del(destThFile);
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        File destFile = null;
        try {
            // 这里还有优化空间，例如跨存储设备或跨分区移动大文件时，会耗时很久且无法监听到进度，
            // 可以使用复制+删除源文件来实现，这样可以监听到进度。
            // 但是正常来说，不应该在单个存储平台使用的存储路径下挂载不同的分区，这会造成维护上的困难，
            // 不同的分区最好用配置成不同的存储平台，除非你明确知道为什么要这么做及可能会出现的问题。
            destFile = FileUtil.touch(getAbsolutePath(destFileKey));
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.length());
            FileUtil.move(srcFile, destFile, true);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.length());
        } catch (Exception e) {
            if (destThFile != null) {
                try {
                    FileUtil.move(destThFile, srcThFile, true);
                } catch (Exception ignored) {
                }
            }
            try {
                if (srcFile.exists()) {
                    FileUtil.del(destFile);
                } else if (destFile != null) {
                    FileUtil.move(destFile, srcFile, true);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }

    /**
     * 本地的分片信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocalPartInfo {
        private Integer partNumber;
        private String etag;
        private Long size;
        private Date lastModified;

        public LocalPartInfo(String text) {
            String[] arr = text.split("_");
            partNumber = Integer.parseInt(arr[0]);
            etag = arr[1];
            size = Long.parseLong(arr[2]);
            lastModified = new Date(Long.parseLong(arr[3]));
        }

        public String toIndexString() {
            return partNumber + "_" + etag + "_" + size + "_" + lastModified.getTime();
        }
    }
}
