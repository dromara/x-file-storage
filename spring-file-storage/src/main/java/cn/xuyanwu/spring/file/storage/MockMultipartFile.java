/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * 一个模拟 MultipartFile 的类
 */
@Getter
public class MockMultipartFile implements MultipartFile {

    /**
     * 文件名
     */
    private final String name;

    /**
     * 原始文件名
     */
    private final String originalFilename;

    /**
     * 内容类型
     */
    @Nullable
    private final String contentType;

    /**
     * 文件内容
     */
    private final byte[] bytes;


    public MockMultipartFile(String name,InputStream in) {
        this(name,"",null,IoUtil.readBytes(in));
    }

    public MockMultipartFile(String name,@Nullable byte[] bytes) {
        this(name,"",null,bytes);
    }

    public MockMultipartFile(String name,@Nullable String originalFilename,@Nullable String contentType,InputStream in) {
        this(name,originalFilename,contentType,IoUtil.readBytes(in));
    }

    public MockMultipartFile(@Nullable String name,@Nullable String originalFilename,@Nullable String contentType,@Nullable byte[] bytes) {
        this.name = (name != null ? name : "");
        this.originalFilename = (originalFilename != null ? originalFilename : "");
        this.contentType = contentType;
        this.bytes = (bytes != null ? bytes : new byte[0]);
    }


    @Override
    public boolean isEmpty() {
        return (this.bytes.length == 0);
    }

    @Override
    public long getSize() {
        return this.bytes.length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public void transferTo(File dest) {
        FileUtil.writeBytes(bytes,dest);
    }

}
