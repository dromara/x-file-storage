package cn.xuyanwu.spring.file.storage.tika;

import org.apache.tika.Tika;

/**
 * 默认的 Tika 工厂类
 */
public class DefaultTikaFactory implements TikaFactory {
    private Tika tika;

    @Override
    public Tika getTika() {
        if (tika == null) {
            tika = new Tika();
        }
        return tika;
    }
}
