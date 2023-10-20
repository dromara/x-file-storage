package org.dromara.x.file.storage.core;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 带进度通知的 InputStream 包装类
 */
public class ProgressInputStream extends FilterInputStream {

    private boolean readFlag;
    private boolean finishFlag;
    private long progressSize;
    private final long allSize;
    private final ProgressListener listener;

    public ProgressInputStream(InputStream in,ProgressListener listener,long allSize) {
        super(in);
        this.listener = listener;
        this.allSize = allSize;
    }

    @Override
    public long skip(long n) throws IOException {
        long skip = super.skip(n);
        progress(skip);
        return skip;
    }


    @Override
    public int read() throws IOException {
        int b = super.read();
        progress(b == -1 ? -1 : 1);
        return b;
    }

    @Override
    public int read(byte[] b,int off,int len) throws IOException {
        if (!this.readFlag) {
            this.readFlag = true;
            this.listener.start();
        }
        int bytes = super.read(b,off,len);
        progress(bytes);
        return bytes;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    protected void progress(long size) {
        if (size > 0) {
            this.listener.progress(progressSize += size,allSize);
        } else if (size < 0) {
            if (!this.finishFlag) {
                this.finishFlag = true;
                this.listener.finish();
            }
        }
    }


}
