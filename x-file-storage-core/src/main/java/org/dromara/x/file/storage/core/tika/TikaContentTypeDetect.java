package org.dromara.x.file.storage.core.tika;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Tika 识别文件的 MIME 类型
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TikaContentTypeDetect implements ContentTypeDetect {
    private TikaFactory tikaFactory;

    @Override
    public String detect(File file) throws IOException {
        return tikaFactory.getTika().detect(file);
    }

    @Override
    public String detect(byte[] bytes) {
        return tikaFactory.getTika().detect(bytes);
    }

    @Override
    public String detect(byte[] bytes,String filename) {
        return tikaFactory.getTika().detect(bytes,filename);
    }

    @Override
    public String detect(InputStream in,String filename) throws IOException {
        return tikaFactory.getTika().detect(in,filename);
    }
}
