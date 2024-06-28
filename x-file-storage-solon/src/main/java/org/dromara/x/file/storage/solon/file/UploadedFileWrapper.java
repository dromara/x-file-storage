package org.dromara.x.file.storage.solon.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.noear.solon.core.handle.UploadedFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

/** Solon UploadedFile 文件包装类 */
@Getter
@Setter
@NoArgsConstructor
public class UploadedFileWrapper implements FileWrapper {

    private UploadedFile file;
    private String name;
    private String contentType;
    private InputStream inputStream;
    private Long size;

    public UploadedFileWrapper(UploadedFile file, String name, String contentType, Long size) {
        this.file = file;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new BufferedInputStream(file.getContent());
        }
        return inputStream;
    }


    @Override
    public void transferTo(final File dest) {
        try {
            file.transferTo(dest);
            IoUtil.close(inputStream);
        } catch (Exception ignored) {
            try {
                FileUtil.writeFromStream(getInputStream(), dest);
            } catch (Exception e) {
                throw new FileStorageRuntimeException("文件移动失败", e);
            }
        }
    }

    @Override
    public boolean supportTransfer() {
        return true;
    }
}
