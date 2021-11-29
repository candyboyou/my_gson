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
 * A structure, name or value type in a JSON-encoded string.
 * <p>
 * JsonToken是一个枚举类型,用来标识在Json数据格式中的各种语法单位。
 *
 * @author Jesse Wilson
 * @since 1.6
 */
public enum JsonToken {

  /**
   * The opening of a JSON array. Written using {@link JsonWriter#beginArray}
   * and read using {@link JsonReader#beginArray}.
   *
   * <p>
   * 标记json数组的开始。使用{@link JsonWriter#beginArray}写入以及{@link JsonReader#beginArray}读取
   */
  BEGIN_ARRAY,

  /**
   * The closing of a JSON array. Written using {@link JsonWriter#endArray}
   * and read using {@link JsonReader#endArray}.
   */
  END_ARRAY,

  /**
   * The opening of a JSON object. Written using {@link JsonWriter#beginObject}
   * and read using {@link JsonReader#beginObject}.
   */
  BEGIN_OBJECT,

  /**
   * The closing of a JSON object. Written using {@link JsonWriter#endObject}
   * and read using {@link JsonReader#endObject}.
   */
  END_OBJECT,

  /**
   * A JSON property name. Within objects, tokens alternate between names and
   * their values. Written using {@link JsonWriter#name} and read using {@link
   * JsonReader#nextName}
   *
   * <p>JSON 的属性名称。 在对象中，token在名称和对应的值之间交替。 使用 {@link JsonWriter#name} 写入，使用 {@link JsonReader#nextName} 阅读
   */
  NAME,

  /**
   * A JSON string.
   *
   * <p>是一个String字符串
   */
  STRING,

  /**
   * A JSON number represented in this API by a Java {@code double}, {@code
   * long}, or {@code int}.
   */
  NUMBER,

  /**
   * A JSON {@code true} or {@code false}.
   */
  BOOLEAN,

  /**
   * A JSON {@code null}.
   */
  NULL,

  /**
   * The end of the JSON stream. This sentinel value is returned by {@link
   * JsonReader#peek()} to signal that the JSON-encoded value has no more
   * tokens.
   */
  END_DOCUMENT
}
