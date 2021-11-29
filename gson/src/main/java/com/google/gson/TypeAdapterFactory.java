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

import com.google.gson.reflect.TypeToken;

/**
 * Creates type adapters for set of related types. Type adapter factories are
 * most useful when several types share similar structure in their JSON form.
 *
 * <p>为一组相关类型创建类型适配器。 当几种类型在其 JSON 格式中共享相似的结构时，类型适配器工厂最有用。
 *
 * <h3>Example: Converting enums to lowercase</h3>
 * In this example, we implement a factory that creates type adapters for all
 * enums. The type adapters will write enums in lowercase, despite the fact
 * that they're defined in {@code CONSTANT_CASE} in the corresponding Java
 * model:
 *
 * <h3>示例：将枚举转换为小写</h3>
 *
 * 在这个例子中，我们实现了一个为枚举创建类型适配器的工厂。
 * 类型适配器将以小写写入枚举，尽管它们是在相应 Java 模型的 {@code CONSTANT_CASE} 中定义的：
 * <pre>   {@code
 *   public class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
 *     public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
 *       Class<T> rawType = (Class<T>) type.getRawType();
 *       if (!rawType.isEnum()) {
 *         return null;
 *       }
 *
 *       final Map<String, T> lowercaseToConstant = new HashMap<String, T>();
 *       for (T constant : rawType.getEnumConstants()) {
 *         lowercaseToConstant.put(toLowercase(constant), constant);
 *       }
 *
 *       return new TypeAdapter<T>() {
 *         public void write(JsonWriter out, T value) throws IOException {
 *           if (value == null) {
 *             out.nullValue();
 *           } else {
 *             out.value(toLowercase(value));
 *           }
 *         }
 *
 *         public T read(JsonReader reader) throws IOException {
 *           if (reader.peek() == JsonToken.NULL) {
 *             reader.nextNull();
 *             return null;
 *           } else {
 *             return lowercaseToConstant.get(reader.nextString());
 *           }
 *         }
 *       };
 *     }
 *
 *     private String toLowercase(Object o) {
 *       return o.toString().toLowerCase(Locale.US);
 *     }
 *   }
 * }</pre>
 *
 * <p>Type adapter factories select which types they provide type adapters
 * for. If a factory cannot support a given type, it must return null when
 * that type is passed to {@link #create}. Factories should expect {@code
 * create()} to be called on them for many types and should return null for
 * most of those types. In the above example the factory returns null for
 * calls to {@code create()} where {@code type} is not an enum.
 *
 * 类型适配器工厂选择它们为哪些类型提供类型适配器。
 * 如果工厂不能支持给定的类型，则它必须在该类型被传递给 {@link #create}情况前返回 null
 * 在上面的示例中，工厂为调用 {@code create()} 返回 null，其中 {@code type} 不是枚举。
 *
 * <p>A factory is typically called once per type, but the returned type
 * adapter may be used many times. It is most efficient to do expensive work
 * like reflection in {@code create()} so that the type adapter's {@code
 * read()} and {@code write()} methods can be very fast. In this example the
 * mapping from lowercase name to enum value is computed eagerly.
 *
 * 一个工厂通常被每个类型调用一次，但返回的类型适配器可能会被多次使用。
 * 在 {@code create()} 中执行像反射这样的昂贵工作是最有效的，这样类型适配器的 {@code read()} 和 {@code write()} 方法可以非常快。
 * 在这个例子中，从小写名称到枚举值的映射是急切地计算的。
 *
 * <p>As with type adapters, factories must be <i>registered</i> with a {@link
 * com.google.gson.GsonBuilder} for them to take effect: <pre>   {@code
 *
 * 和类型适配器一样，工厂也是必须要注册才能生效
 *
 *  GsonBuilder builder = new GsonBuilder();
 *  builder.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory());
 *  ...
 *  Gson gson = builder.create();
 * }</pre>
 * If multiple factories support the same type, the factory registered earlier
 * takes precedence.
 *
 * 如果多个工厂支持同一类型，则以较早注册的工厂为准。
 *
 * <h3>Example: composing other type adapters</h3>
 * In this example we implement a factory for Guava's {@code Multiset}
 * collection type. The factory can be used to create type adapters for
 * multisets of any element type: the type adapter for {@code
 * Multiset<String>} is different from the type adapter for {@code
 * Multiset<URL>}.
 *
 * <h3>示例：组合其他类型的适配器</h3>
 * 在这个例子中，我们为 Guava 的 {@code Multiset} 集合类型实现了一个工厂。
 * 工厂可用于为任何元素类型的多重集创建类型适配器：
 * {@code Multiset<String>} 的类型适配器与 {@code Multiset<URL>} 的类型适配器不同。
 *
 * <p>The type adapter <i>delegates</i> to another type adapter for the
 * multiset elements. It figures out the element type by reflecting on the
 * multiset's type token. A {@code Gson} is passed in to {@code create} for
 * just this purpose:
 * <p>类型适配器<i>委托</i>多集元素的另一个类型适配器。
 * 它通过反映多重集的类型标记来计算元素类型。
 * 一个 {@code Gson} 被传递给 {@code create} 就是为了这个目的：
 * <pre> {@code
 *
 *   public class MultisetTypeAdapterFactory implements TypeAdapterFactory {
 *     public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
 *       Type type = typeToken.getType();
 *       if (typeToken.getRawType() != Multiset.class
 *           || !(type instanceof ParameterizedType)) {
 *         return null;
 *       }
 *
 *       Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
 *       TypeAdapter<?> elementAdapter = gson.getAdapter(TypeToken.get(elementType));
 *       return (TypeAdapter<T>) newMultisetAdapter(elementAdapter);
 *     }
 *
 *     private <E> TypeAdapter<Multiset<E>> newMultisetAdapter(
 *         final TypeAdapter<E> elementAdapter) {
 *       return new TypeAdapter<Multiset<E>>() {
 *         public void write(JsonWriter out, Multiset<E> value) throws IOException {
 *           if (value == null) {
 *             out.nullValue();
 *             return;
 *           }
 *
 *           out.beginArray();
 *           for (Multiset.Entry<E> entry : value.entrySet()) {
 *             out.value(entry.getCount());
 *             elementAdapter.write(out, entry.getElement());
 *           }
 *           out.endArray();
 *         }
 *
 *         public Multiset<E> read(JsonReader in) throws IOException {
 *           if (in.peek() == JsonToken.NULL) {
 *             in.nextNull();
 *             return null;
 *           }
 *
 *           Multiset<E> result = LinkedHashMultiset.create();
 *           in.beginArray();
 *           while (in.hasNext()) {
 *             int count = in.nextInt();
 *             E element = elementAdapter.read(in);
 *             result.add(element, count);
 *           }
 *           in.endArray();
 *           return result;
 *         }
 *       };
 *     }
 *   }
 * }</pre>
 * Delegating from one type adapter to another is extremely powerful; it's
 * the foundation of how Gson converts Java objects and collections. Whenever
 * possible your factory should retrieve its delegate type adapter in the
 * {@code create()} method; this ensures potentially-expensive type adapter
 * creation happens only once.
 *
 * @since 2.1
 */
public interface TypeAdapterFactory {

  /**
   * Returns a type adapter for {@code type}, or null if this factory doesn't
   * support {@code type}.
   * <p> 只有一个方法，用于根据解析器和变量类型来创建 TypeAdapter </p>
   */
  <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type);
}
