/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.stream;

/**
 * Lexical scoping elements within a JSON reader or writer.
 *
 * @author Jesse Wilson
 * @since 1.6
 */
final class JsonScope {

    /**
     * An array with no elements requires no separators or newlines before
     * it is closed.
     */
    static final int EMPTY_ARRAY = 1;

    /**
     * A array with at least one value requires a comma and newline before
     * the next element.
     */
    static final int NONEMPTY_ARRAY = 2;

    /**
     * An object with no name/value pairs requires no separators or newlines
     * before it is closed.
     */
    static final int EMPTY_OBJECT = 3;

    /**
     * An object whose most recent element is a key. The next element must
     * be a value.
     *
     * 当前是一个key，下一个元素一定是值。
     */
    static final int DANGLING_NAME = 4;

    /**
     * An object with at least one name/value pair requires a comma and
     * newline before the next element.
     * <p>
     * 当前是一个value，下一个元素不是逗号就是"}"
     */
    static final int NONEMPTY_OBJECT = 5;

    /**
     * No object or array has been started.
     * <p>初始状态，啥也没读</p>
     */
    static final int EMPTY_DOCUMENT = 6;

    /**
     * A document with at an array or object.
     *
     * Json字符串中有一个对象或者数组
     */
    static final int NONEMPTY_DOCUMENT = 7;

    /**
     * A document that's been closed and cannot be accessed.
     */
    static final int CLOSED = 8;
}
