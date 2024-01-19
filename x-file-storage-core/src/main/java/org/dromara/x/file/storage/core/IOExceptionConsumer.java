/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.dromara.x.file.storage.core;

import java.io.IOException;

/**
 * 带 IOException 异常的 Consumer
 */
@FunctionalInterface
public interface IOExceptionConsumer<T> {

    void accept(T t) throws IOException;
}
