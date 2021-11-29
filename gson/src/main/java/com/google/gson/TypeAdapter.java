/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.gson;

import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Converts Java objects to and from JSON.
 *
 * <h3>Defining a type's JSON form</h3>
 * By default Gson converts application classes to JSON using its built-in type
 * adapters. If Gson's default JSON conversion isn't appropriate for a type,
 * extend this class to customize the conversion. Here's an example of a type
 * adapter for an (X,Y) coordinate point: <pre>   {@code
 *
 * 这个类的作用主要是将Java对象与Json进行转换
 *
 * <h3>规定一种Json的类型形式</h3>
 *
 * 默认情况下，Gson 使用其内置类型适配器将应用程序类转换为 JSON。
 *
 * 如果 Gson 的默认 JSON 转换不适用于某个类型，请扩展此类以自定义转换。以下是 (X,Y) 坐标点的类型适配器示例：
 *
 *   public class PointAdapter extends TypeAdapter<Point> {
 *     // 反序列化为对象
 *     public Point read(JsonReader reader) throws IOException {
 *       if (reader.peek() == JsonToken.NULL) {
 *         reader.nextNull();
 *         return null;
 *       }
 *       String xy = reader.nextString();
 *       String[] parts = xy.split(",");
 *       int x = Integer.parseInt(parts[0]);
 *       int y = Integer.parseInt(parts[1]);
 *       return new Point(x, y);
 *     }
 *     // 转为json
 *     public void write(JsonWriter writer, Point value) throws IOException {
 *       if (value == null) {
 *         writer.nullValue();
 *         return;
 *       }
 *       String xy = value.getX() + "," + value.getY();
 *       writer.value(xy);
 *     }
 *   }}</pre>
 * With this type adapter installed, Gson will convert {@code Points} to JSON as
 * strings like {@code "5,8"} rather than objects like {@code {"x":5,"y":8}}. In
 * this case the type adapter binds a rich Java class to a compact JSON value.
 *
 * <p>安装这种类型的适配器后，Gson 会将 {@code Points} 转换为 JSON 作为字符串，
 * 如 {@code "5,8"} 而不是像 {@code {"x":5,"y":8}} 这样的对象。
 * 在这种情况下，类型适配器将丰富的 Java 类绑定到紧凑的 JSON 值。
 *
 * <p>The {@link #read(JsonReader) read()} method must read exactly one value
 * and {@link #write(JsonWriter,Object) write()} must write exactly one value.
 * For primitive types this is means readers should make exactly one call to
 * {@code nextBoolean()}, {@code nextDouble()}, {@code nextInt()}, {@code
 * nextLong()}, {@code nextString()} or {@code nextNull()}. Writers should make
 * exactly one call to one of <code>value()</code> or <code>nullValue()</code>.
 * For arrays, type adapters should start with a call to {@code beginArray()},
 * convert all elements, and finish with a call to {@code endArray()}. For
 * objects, they should start with {@code beginObject()}, convert the object,
 * and finish with {@code endObject()}. Failing to convert a value or converting
 * too many values may cause the application to crash.
 *
 * <p>{@link #read(JsonReader) read()} 方法必须只读取一个值，{@link #write(JsonWriter, Object) write()} 必须只写入一个值。
 * <p>对于原始类型，这意味着读者应该只调用一次 {@code nextBoolean()}、{@code nextDouble()}、{@code nextInt()}、{@code nextLong()}、{@code nextString( )} 或 {@code nextNull()}。
 * 编写者应该只对 <code>value()</code> 或 <code>nullValue()</code> 之一进行一次调用。
 * <p>对于数组，类型适配器应该以调用 {@code beginArray()} 开始，转换所有元素，并以调用 {@code endArray()} 结束。
 * <p>对于对象，它们应该以 {@code beginObject()} 开始，转换对象，并以 {@code endObject()} 结束。
 * 未能转换一个值或转换太多的值可能会导致应用程序崩溃。
 *
 * <p>Type adapters should be prepared to read null from the stream and write it
 * to the stream. Alternatively, they should use {@link #nullSafe()} method while
 * registering the type adapter with Gson. If your {@code Gson} instance
 * has been configured to {@link GsonBuilder#serializeNulls()}, these nulls will be
 * written to the final document. Otherwise the value (and the corresponding name
 * when writing to a JSON object) will be omitted automatically. In either case
 * your type adapter must handle null.
 *
 * <p>类型适配器应该处理好从流中读取 null 并将其写入流。 或者，他们应该在向 Gson 注册类型适配器时使用 {@link #nullSafe()} 方法。
 * <p>如果您的 {@code Gson} 实例已配置为 {@link GsonBuilder#serializeNulls()}，则这些空值将写入最终文档。
 * <p>否则该值（以及写入 JSON 对象时的相应名称）将被自动省略。
 * <p>不管怎么样，在任何一种情况下，您的类型适配器都必须处理 null。
 *
 * <p>To use a custom type adapter with Gson, you must <i>register</i> it with a
 * {@link GsonBuilder}:
 *
 * <p>要在 Gson 中使用自定义类型适配器，您必须使用 {@link GsonBuilder} <i>注册</i>它
 *
 * <pre>   {@code
 *
 *   GsonBuilder builder = new GsonBuilder();
 *   builder.registerTypeAdapter(Point.class, new PointAdapter());
 *   // if PointAdapter didn't check for nulls in its read/write methods, you should instead use
 *   // builder.registerTypeAdapter(Point.class, new PointAdapter().nullSafe());
 *   ...
 *   Gson gson = builder.create();
 * }</pre>
 *
 * @since 2.1
 */
// non-Javadoc:
//
// <h3>JSON Conversion</h3>
// <p>A type adapter registered with Gson is automatically invoked while serializing
// or deserializing JSON. However, you can also use type adapters directly to serialize
// and deserialize JSON. Here is an example for deserialization: <pre>   {@code
//
//   String json = "{'origin':'0,0','points':['1,2','3,4']}";
//   TypeAdapter<Graph> graphAdapter = gson.getAdapter(Graph.class);
//   Graph graph = graphAdapter.fromJson(json);
// }</pre>
// And an example for serialization: <pre>   {@code
//
//   Graph graph = new Graph(...);
//   TypeAdapter<Graph> graphAdapter = gson.getAdapter(Graph.class);
//   String json = graphAdapter.toJson(graph);
// }</pre>
//
// <p>Type adapters are <strong>type-specific</strong>. For example, a {@code
// TypeAdapter<Date>} can convert {@code Date} instances to JSON and JSON to
// instances of {@code Date}, but cannot convert any other types.
//
public abstract class TypeAdapter<T> {

  /**
   * Writes one JSON value (an array, object, string, number, boolean or null)
   * for {@code value}.
   *
   * @param value the Java object to write. May be null.
   */
  public abstract void write(JsonWriter out, T value) throws IOException;

  /**
   * Converts {@code value} to a JSON document and writes it to {@code out}.
   * Unlike Gson's similar {@link Gson#toJson(JsonElement, Appendable) toJson}
   * method, this write is strict. Create a {@link
   * JsonWriter#setLenient(boolean) lenient} {@code JsonWriter} and call
   * {@link #write(com.google.gson.stream.JsonWriter, Object)} for lenient
   * writing.
   *
   * @param value the Java object to convert. May be null.
   * @since 2.2
   */
  public final void toJson(Writer out, T value) throws IOException {
    JsonWriter writer = new JsonWriter(out);
    write(writer, value);
  }

  /**
   * This wrapper method is used to make a type adapter null tolerant. In general, a
   * type adapter is required to handle nulls in write and read methods.
   *
   * <p>这个方法就是让类型适配器处理写入和读取方法中的空值
   *
   * <p>Here is how this is typically done:<br>
   * <pre>   {@code
   * Gson gson = new GsonBuilder().registerTypeAdapter(Foo.class,
   *   new TypeAdapter<Foo>() {
   *     public Foo read(JsonReader in) throws IOException {
   *       if (in.peek() == JsonToken.NULL) {
   *         in.nextNull();
   *         return null;
   *       }
   *       // read a Foo from in and return it
   *     }
   *     public void write(JsonWriter out, Foo src) throws IOException {
   *       if (src == null) {
   *         out.nullValue();
   *         return;
   *       }
   *       // write src as JSON to out
   *     }
   *   }).create();
   * }</pre>
   * You can avoid this boilerplate handling of nulls by wrapping your type adapter with
   * this method.
   * <p>现在可以通过这个方法来避免对null值进行手动的判断了
   * <p>Here is how we will rewrite the above example:
   * <pre>   {@code
   * Gson gson = new GsonBuilder().registerTypeAdapter(Foo.class,
   *   new TypeAdapter<Foo>() {
   *     public Foo read(JsonReader in) throws IOException {
   *       // read a Foo from in and return it
   *     }
   *     public void write(JsonWriter out, Foo src) throws IOException {
   *       // write src as JSON to out
   *     }
   *   }.nullSafe()).create();
   * }</pre>
   * <p>Note that we didn't need to check for nulls in our type adapter after we used nullSafe.
   * <p>请注意，在使用 nullSafe 后，我们就不需要检查类型适配器中的空值了。
   */
  public final TypeAdapter<T> nullSafe() {
    return new TypeAdapter<T>() {
      @Override public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
          out.nullValue();
        } else {
          TypeAdapter.this.write(out, value);
        }
      }
      @Override public T read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
          reader.nextNull();
          return null;
        }
        return TypeAdapter.this.read(reader);
      }
    };
  }

  /**
   * Converts {@code value} to a JSON document. Unlike Gson's similar {@link
   * Gson#toJson(Object) toJson} method, this write is strict. Create a {@link
   * JsonWriter#setLenient(boolean) lenient} {@code JsonWriter} and call
   * {@link #write(com.google.gson.stream.JsonWriter, Object)} for lenient
   * writing.
   *
   * <p>将 {@code value} 转换为 JSON 字符串。
   * <p>与 Gson 类似的 {@link Gson#toJson(Object) toJson} 方法不同，这种writer是严格要求的。
   * <p>创建一个 {@link JsonWriter#setLenient(boolean) lenient} {@code JsonWriter} 并调用 {@link #write(com.google.gson.stream.JsonWriter, Object)} 可以进行宽松的处理。
   *
   * @param value the Java object to convert. May be null.
   * @since 2.2
   */
  public final String toJson(T value) {
    StringWriter stringWriter = new StringWriter();
    try {
      toJson(stringWriter, value);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to a StringWriter.
    }
    return stringWriter.toString();
  }

  /**
   * Converts {@code value} to a JSON tree.
   * <p>转换value为一个Json树
   * @param value the Java object to convert. May be null.
   * @return the converted JSON tree. May be {@link JsonNull}.
   * @since 2.2
   */
  public final JsonElement toJsonTree(T value) {
    try {
      JsonTreeWriter jsonWriter = new JsonTreeWriter();
      write(jsonWriter, value);
      return jsonWriter.get();
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }

  /**
   * Reads one JSON value (an array, object, string, number, boolean or null)
   * and converts it to a Java object. Returns the converted object.
   *
   * <p>这是一个抽象方法。读取一个 JSON 值（数组、对象、字符串、数字、布尔值或 null）并将其转换为 Java 对象。 返回转换后的对象。
   *
   * @return the converted Java object. May be null.
   */
  public abstract T read(JsonReader in) throws IOException;

  /**
   * Converts the JSON document in {@code in} to a Java object. Unlike Gson's
   * similar {@link Gson#fromJson(java.io.Reader, Class) fromJson} method, this
   * read is strict. Create a {@link JsonReader#setLenient(boolean) lenient}
   * {@code JsonReader} and call {@link #read(JsonReader)} for lenient reading.
   *
   * @return the converted Java object. May be null.
   * @since 2.2
   */
  public final T fromJson(Reader in) throws IOException {
    JsonReader reader = new JsonReader(in);
    return read(reader);
  }

  /**
   * Converts the JSON document in {@code json} to a Java object. Unlike Gson's
   * similar {@link Gson#fromJson(String, Class) fromJson} method, this read is
   * strict. Create a {@link JsonReader#setLenient(boolean) lenient} {@code
   * JsonReader} and call {@link #read(JsonReader)} for lenient reading.
   *
   * @return the converted Java object. May be null.
   * @since 2.2
   */
  public final T fromJson(String json) throws IOException {
    return fromJson(new StringReader(json));
  }

  /**
   * Converts {@code jsonTree} to a Java object.
   *
   * @param jsonTree the Java object to convert. May be {@link JsonNull}.
   * @since 2.2
   */
  public final T fromJsonTree(JsonElement jsonTree) {
    try {
      JsonReader jsonReader = new JsonTreeReader(jsonTree);
      return read(jsonReader);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }
}
