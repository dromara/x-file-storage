package org.dromara.x.file.storage.core.file;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

/**
 * multipart/form-data 读取器
 */
public class MultipartFormDataReader {
    /**
     * 读取时缓冲区长度，默认 128KB
     */
    public static int BUFFER_LENGTH = 128 * 1024;

    /**
     * 读取 HttpServletRequest 中 InputStream 的数据，仅支持 multipart/form-data 格式的请求，需要注意以下几点：
     * 1.要上传的文件参数位置必须是最后一个。
     * 2.如果要有缩略图文件一起上传，缩略图所在的参数位置必须为倒数第二个，且必须传入参数 _hasTh 值为 true。
     * 3.要上传的文件大小会自动推断，但是某些情况下可能推断错误，导致上传的文件出现问题，所以最好传入 _fileSize 参数，值为文件大小
     * 4.除此之外不能出现任何文件参数
     *
     * @param contentType   请求类型
     * @param inputStream   输入流
     * @param charset       字符集
     * @param contentLength 请求正文部分的长度
     */
    public static MultipartFormData read(
            String contentType, InputStream inputStream, Charset charset, Long contentLength) throws IOException {

        String boundary = Boundary.getBoundary(contentType);
        if (boundary == null)
            throw new FileStorageRuntimeException("HttpServletRequest 的 ContentType 中未读取到 boundary 参数！");
        MultipartFormData data = new MultipartFormData();
        data.inputStream = IoUtil.toMarkSupportStream(inputStream);
        data.charset = charset;
        data.contentLength = contentLength;
        data.buffer = new byte[BUFFER_LENGTH];
        data.parameterMap = new LinkedHashMap<>();

        // multipart/form-data; boundary=----WebKitFormBoundary0iQfWrHD6Yl9PNRe

        // ------WebKitFormBoundary0iQfWrHD6Yl9PNRe
        // Content-Disposition: form-data; name="isPrivate"
        //
        // false
        // ------WebKitFormBoundary0iQfWrHD6Yl9PNRe
        // Content-Disposition: form-data; name="saveFilename"
        //
        //
        // ------WebKitFormBoundary0iQfWrHD6Yl9PNRe
        // Content-Disposition: form-data; name="files"; filename="a.png"
        // Content-Type: image/png
        //
        //
        // ------WebKitFormBoundary0iQfWrHD6Yl9PNRe--
        //

        // 读取Part分隔行
        do {
            data.boundary = Boundary.create(readLineBytes(data), boundary, charset);
        } while (data.boundary == null);

        // 循环读取每个 Part 直到找到要上传的文件为止
        while (true) {
            if (readPart(data) == 3) break;
        }
        return data;
    }

    /**
     * 读取 Part 并返回类型：1普通参数，2缩略图文件，3要上传的文件
     */
    public static int readPart(MultipartFormData data) throws IOException {
        HashMap<String, String> headerMap = new HashMap<>();
        // 读取 Part 的 Header 部分
        while (true) {
            String line = new String(readLineBytes(data), data.charset);
            if (StrUtil.isBlank(line)) break; // 头部已读完
            int splitIndex = line.indexOf(": ");
            if (splitIndex < 0) {
                headerMap.put(line.trim().toLowerCase(), "");
            } else {
                String name = line.substring(0, splitIndex).trim().toLowerCase();
                String value = line.substring(splitIndex + 1).trim();
                headerMap.put(name, value);
            }
        }

        // 读取解析 Part 的内容定义参数
        String disposition = headerMap.get("content-disposition");
        if (StrUtil.isEmpty(disposition))
            throw new FileStorageRuntimeException("HttpServletRequest 的 Part 无法识别 content-disposition");
        LinkedHashMap<String, String> dispositionMap = convertPartHeaderValue(disposition);
        MultipartFormDataPartInputStream pin = new MultipartFormDataPartInputStream(data);

        if (dispositionMap.containsKey("filename")) { // 此参数有值，表示这部分是个文件
            if ("true".equals(data.getParameter("_hasTh")) && data.thFileBytes == null) { // 缩略图文件
                data.thFileContentType = headerMap.get("content-type");
                data.thFileBytes = IoUtil.readBytes(pin);
                data.thFileOriginalFilename = dispositionMap.get("filename");
                return 2;
            } else { // 要上传文件文件主体
                data.fileContentType = headerMap.get("content-type");
                data.fileInputStream = pin;
                data.fileOriginalFilename = dispositionMap.get("filename");
                // 这里处理文件大小，如果参数中提供了，则使用参数中的
                // 否则通过流的总长度减去已读取长度和最后一个分割行及空行的长度，从而推算出这个文件的大小
                // 但是这样有个弊端，就是这个文件的参数位置必须是最后一个，否则将计算错误
                String fileSize = data.getParameter("_fileSize");
                if (StrUtil.isNotBlank(fileSize)) {
                    data.fileSize = Long.parseLong(fileSize);
                } else {
                    data.fileSize = data.contentLength - data.totalReadLength - data.boundary.footerByteLength;
                }
                return 3;
            }
        } else { // 解析成普通参数
            String name = dispositionMap.get("name");
            String value = IoUtil.read(pin, data.charset);
            String[] values = data.parameterMap.get(name);
            values = values == null ? new String[] {value} : ArrayUtil.append(values, value);
            data.parameterMap.put(name, values);
            return 1;
        }
    }

    /**
     * 简易版的 Header 值解析方法，直接按照分隔符解析，如果分隔符是参数名称或值的一部分，则解析会出现问题
     */
    public static LinkedHashMap<String, String> convertPartHeaderValue(String text) {
        return Arrays.stream(text.split(";"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(v -> {
                    String name, value;
                    int splitIndex = v.indexOf("=");
                    if (splitIndex < 0) {
                        name = v.trim().toLowerCase();
                        value = "";
                    } else {
                        name = v.substring(0, splitIndex).trim().toLowerCase();
                        value = v.substring(splitIndex + 1).trim();
                        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }
                    return new String[] {name, value};
                })
                .collect(Collectors.toMap(v -> v[0], v -> v[1], (o, n) -> n, LinkedHashMap::new));
    }

    /**
     * 读入一行字节数组
     */
    public static byte[] readLineBytes(MultipartFormData data) throws IOException {
        int readLength = readLine(data.inputStream, data.buffer, 0, data.buffer.length);
        if (readLength == -1) throw new FileStorageRuntimeException("HttpServletRequest 解析失败，尚未发现文件");
        data.totalReadLength += readLength;
        if (readLength == data.buffer.length)
            throw new FileStorageRuntimeException("HttpServletRequest 解析失败，参数超过缓冲区大小");
        return Arrays.copyOfRange(data.buffer, 0, readLength);
    }

    /**
     * 读取输入流，一次读取一行。从偏移量开始，将字节读入数组，直到读取一定数量的字节或到达换行符，该换行符也会读入数组
     *
     * <p>如果此方法在读取最大字节数之前到达输入流的末尾，则返回 -1
     *
     * @param in  输入流
     * @param b   读取数据的字节数组
     * @param off 一个整数，指定此方法开始读取的字符
     * @param len 指定要读取的最大字节数的整数
     * @return 一个整数，指定实际读取的字节数，如果到达流的末尾，则为 -1
     * @throws IOException 如果发生输入或输出异常
     */
    public static int readLine(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len <= 0) return 0;
        int count = 0, c;
        while ((c = in.read()) != -1) {
            b[off++] = (byte) c;
            count++;
            if (c == '\n' || count == len) break;
        }
        return count > 0 ? count : -1;
    }

    /**
     * 用于读取 MultipartFormData 中 Part 的内容的 InputStream
     */
    public static class MultipartFormDataPartInputStream extends InputStream {
        private final MultipartFormData data;
        private int bufferLength = 0;
        private int index = -1;
        /**
         * 状态，0未读取，1读取中，2已读完
         */
        private int status = 0;

        public MultipartFormDataPartInputStream(MultipartFormData data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            if (index + 1 == bufferLength) { // 当前缓冲区已读完
                if (status == 2) return -1;
                readLineBuffer();
                if (index + 1 == bufferLength && status == 2) return -1;
            }
            return data.buffer[++index] & 0xff;
        }

        protected void readLineBuffer() throws IOException {
            if (status == 2) return;
            int readLength = readLine(data.inputStream, data.buffer, 0, data.buffer.length);
            if (readLength == -1) throw new FileStorageRuntimeException("HttpServletRequest 解析失败，文件尚未完整读取");

            data.totalReadLength += readLength;
            // 判断是否为结束行
            if (isEndLine(data.buffer, readLength)) {
                status = 2;
                return;
            }

            bufferLength = readLength;
            index = -1;

            // 如果当前读取的数据是以换行符结尾的，则判断下一行是否为结束行
            if (endsWithLineEndFlag(data.buffer, readLength) && nextLineIsEndLine()) {
                status = 2;
                bufferLength -= data.boundary.lineEndFlagBytes.length;
            }
        }

        /**
         * 下一行是结束行
         */
        protected boolean nextLineIsEndLine() throws IOException {
            data.inputStream.mark(data.boundary.endLineBytes.length);
            byte[] bytes = new byte[data.boundary.endLineBytes.length];
            int readLength = readLine(data.inputStream, bytes, 0, bytes.length);
            data.inputStream.reset();
            return isEndLine(bytes, readLength);
        }

        /**
         * 是否以行结束符为结尾
         */
        protected boolean endsWithLineEndFlag(byte[] buffer, int readLength) {
            if (readLength < data.boundary.lineEndFlagBytes.length) return false;
            byte[] bytes = Arrays.copyOfRange(buffer, readLength - data.boundary.lineEndFlagBytes.length, readLength);
            return Arrays.equals(bytes, data.boundary.lineEndFlagBytes);
        }

        /**
         * 是否为 Part 结束行
         */
        protected boolean isEndLine(byte[] buffer, int readLength) {
            if (readLength == data.boundary.lineBytes.length
                    && Arrays.equals(Arrays.copyOfRange(buffer, 0, readLength), data.boundary.lineBytes)) {
                return true;
            } else
                return readLength == data.boundary.endLineBytes.length
                        && Arrays.equals(Arrays.copyOfRange(buffer, 0, readLength), data.boundary.endLineBytes);
        }
    }

    @Getter
    public static class Boundary {
        private String boundary;
        private String line;
        private byte[] lineBytes;
        private byte[] endLineBytes;
        private String endLine;
        private int footerByteLength;
        private String lineEndFlag;
        private byte[] lineEndFlagBytes;

        public static Boundary create(byte[] bytes, String boundary, Charset charset) {
            String line = new String(bytes, charset);
            if (!line.contains(boundary)) return null;
            Boundary instance = new Boundary();
            instance.boundary = boundary;
            instance.line = line;
            instance.lineBytes = bytes;

            instance.lineEndFlag = line.endsWith("\r\n") ? "\r\n" : "\n";
            instance.lineEndFlagBytes = instance.lineEndFlag.getBytes(charset);

            instance.endLine =
                    line.substring(0, line.length() - instance.lineEndFlag.length()) + "--" + instance.lineEndFlag;
            instance.endLineBytes = instance.endLine.getBytes(charset);
            instance.footerByteLength = (instance.endLine + instance.lineEndFlag).getBytes(charset).length;
            return instance;
        }

        /**
         * 从 contentType 中获取 boundary 参数
         * multipart/form-data; boundary=----WebKitFormBoundary0iQfWrHD6Yl9PNRe
         */
        public static String getBoundary(String contentType) {
            if (contentType == null) return null;
            int begin = contentType.indexOf("boundary=");
            if (begin < 0) return null;
            int end = contentType.indexOf(";", begin);
            if (end < 0) end = contentType.length();
            begin += "boundary=".length();
            return contentType.substring(begin, end).trim();
        }
    }

    @Getter
    public static class MultipartFormData {
        private InputStream inputStream;
        private Boundary boundary;
        private Charset charset;
        private Long contentLength;
        private byte[] buffer;
        private long totalReadLength = 0L;
        private Map<String, String[]> parameterMap;
        /**
         * 缩略图字节数组
         */
        private byte[] thFileBytes;
        /**
         * 缩略图 MIME 类型
         */
        private String thFileContentType;
        /**
         * 缩略图原始文件名
         */
        private String thFileOriginalFilename;
        /**
         * 文件的输入流
         */
        private InputStream fileInputStream;
        /**
         * 文件 MIME 类型
         */
        private String fileContentType;
        /**
         * 原始文件名
         */
        private String fileOriginalFilename;
        /**
         * 文件大小
         */
        private Long fileSize;

        /**
         * 获取参数值
         */
        public String getParameter(String name) {
            if (parameterMap == null) return null;
            String[] values = parameterMap.get(name);
            if (values == null || values.length == 0) return null;
            return values[0];
        }

        /**
         * 获取多个参数值
         */
        public String[] getParameterValues(String name) {
            if (parameterMap == null) return null;
            return parameterMap.get(name);
        }
    }
}
