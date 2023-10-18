package org.dromara.x.file.storage.core.tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 识别文件的 MIME 类型
 */
public interface ContentTypeDetect {

    String detect(File file) throws IOException;

    String detect(byte[] bytes);

    String detect(byte[] bytes,String filename);

    String detect(InputStream in,String filename) throws IOException;
}
