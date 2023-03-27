package cn.xuyanwu.spring.file.storage.exception;

/**
 * FileStorage 运行时异常
 */
public class FileStorageRuntimeException extends RuntimeException {
    public FileStorageRuntimeException() {
    }

    public FileStorageRuntimeException(String message) {
        super(message);
    }

    public FileStorageRuntimeException(String message,Throwable cause) {
        super(message,cause);
    }

    public FileStorageRuntimeException(Throwable cause) {
        super(cause);
    }

    public FileStorageRuntimeException(String message,Throwable cause,boolean enableSuppression,boolean writableStackTrace) {
        super(message,cause,enableSuppression,writableStackTrace);
    }
}
