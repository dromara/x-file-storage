package org.dromara.x.file.storage.test.resolver;

import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.http.HttpServletRequest;

public class LazyStandardServletMultipartResolver implements MultipartResolver {



    @Override
    public boolean isMultipart(HttpServletRequest request) {
        return false;
    }

    @Override
    public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
        return null;
    }

    @Override
    public void cleanupMultipart(MultipartHttpServletRequest request) {

    }
}
