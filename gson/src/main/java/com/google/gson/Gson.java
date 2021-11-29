/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.*;
import com.google.gson.internal.sql.SqlTypesSupport;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * This is the main class for using Gson. Gson is typically used by first constructing a
 * Gson instance and then invoking {@link #toJson(Object)} or {@link #fromJson(String, Class)}
 * methods on it. Gson instances are Thread-safe so you can reuse them freely across multiple
 * threads.
 *
 * <p>You can create a Gson instance by invoking {@code new Gson()} if the default configuration
 * is all you need. You can also use {@link GsonBuilder} to build a Gson instance with various
 * configuration options such as versioning support, pretty printing, custom
 * {@link JsonSerializer}s, {@link JsonDeserializer}s, and {@link InstanceCreator}s.</p>
 *
 * <p>Here is an example of how Gson is used for a simple Class:
 *
 * <pre>
 * Gson gson = new Gson(); // Or use new GsonBuilder().create();
 * MyType target = new MyType();
 * String json = gson.toJson(target); // serializes target to Json
 * MyType target2 = gson.fromJson(json, MyType.class); // deserializes json into target2
 * </pre></p>
 *
 * <p>If the object that your are serializing/deserializing is a {@code ParameterizedType}
 * (i.e. contains at least one type parameter and may be an array) then you must use the
 * {@link #toJson(Object, Type)} or {@link #fromJson(String, Type)} method. Here is an
 * example for serializing and deserializing a {@code ParameterizedType}:
 *
 * <pre>
 * Type listType = new TypeToken&lt;List&lt;String&gt;&gt;() {}.getType();
 * List&lt;String&gt; target = new LinkedList&lt;String&gt;();
 * target.add("blah");
 *
 * Gson gson = new Gson();
 * String json = gson.toJson(target, listType);
 * List&lt;String&gt; target2 = gson.fromJson(json, listType);
 * </pre></p>
 *
 * <p>See the <a href="https://sites.google.com/site/gson/gson-user-guide">Gson User Guide</a>
 * for a more complete set of examples.</p>
 *
 * @see com.google.gson.reflect.TypeToken
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class Gson {
  static final boolean DEFAULT_JSON_NON_EXECUTABLE = false;
  static final boolean DEFAULT_LENIENT = false;
  static final boolean DEFAULT_PRETTY_PRINT = false;
  static final boolean DEFAULT_ESCAPE_HTML = true;
  static final boolean DEFAULT_SERIALIZE_NULLS = false;
  static final boolean DEFAULT_COMPLEX_MAP_KEYS = false;
  static final boolean DEFAULT_SPECIALIZE_FLOAT_VALUES = false;

  private static final TypeToken<?> NULL_KEY_SURROGATE = TypeToken.get(Object.class);
  private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";

  /**
   * This thread local guards against reentrant calls to getAdapter(). In
   * certain object graphs, creating an adapter for a type may recursively
   * require an adapter for the same type! Without intervention, the recursive
   * lookup would stack overflow. We cheat by returning a proxy type adapter.
   * The proxy is wired up once the initial adapter has been created.
   *
   * <p>这个threadLocal是为了防止对 getAdapter() 的可重入调用。
   * 在某些对象图中，为一个类创建适配器可能会递归调用相同类型的适配器！
   * 如果没有干预，递归查找将堆栈溢出。
   * 我们通过返回代理类型适配器来作弊。
   * 一旦创建了初始适配器，这些代理就会关联起来
   */
  private final ThreadLocal<Map<TypeToken<?>, FutureTypeAdapter<?>>> calls
      = new ThreadLocal<Map<TypeToken<?>, FutureTypeAdapter<?>>>();
  // 使用concurrentHashMap作为缓存
  private final Map<TypeToken<?>, TypeAdapter<?>> typeTokenCache = new ConcurrentHashMap<TypeToken<?>, TypeAdapter<?>>();

  private final ConstructorConstructor constructorConstructor;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

  final List<TypeAdapterFactory> factories;

  final Excluder excluder;
  final FieldNamingStrategy fieldNamingStrategy;
  final Map<Type, InstanceCreator<?>> instanceCreators;
  final boolean serializeNulls;
  final boolean complexMapKeySerialization;
  final boolean generateNonExecutableJson;
  final boolean htmlSafe;
  final boolean prettyPrinting;
  final boolean lenient;
  final boolean serializeSpecialFloatingPointValues;
  final String datePattern;
  final int dateStyle;
  final int timeStyle;
  final LongSerializationPolicy longSerializationPolicy;
  final List<TypeAdapterFactory> builderFactories;
  final List<TypeAdapterFactory> builderHierarchyFactories;
  final ToNumberStrategy objectToNumberStrategy;
  final ToNumberStrategy numberToNumberStrategy;

  /**
   * Constructs a Gson object with default configuration. The default configuration has the
   * following settings:
   * <ul>
   *   <li>The JSON generated by <code>toJson</code> methods is in compact representation. This
   *   means that all the unneeded white-space is removed. You can change this behavior with
   *   {@link GsonBuilder#setPrettyPrinting()}. </li>
   *   <li>The generated JSON omits all the fields that are null. Note that nulls in arrays are
   *   kept as is since an array is an ordered list. Moreover, if a field is not null, but its
   *   generated JSON is empty, the field is kept. You can configure Gson to serialize null values
   *   by setting {@link GsonBuilder#serializeNulls()}.</li>
   *   <li>Gson provides default serialization and deserialization for Enums, {@link Map},
   *   {@link java.net.URL}, {@link java.net.URI}, {@link java.util.Locale}, {@link java.util.Date},
   *   {@link java.math.BigDecimal}, and {@link java.math.BigInteger} classes. If you would prefer
   *   to change the default representation, you can do so by registering a type adapter through
   *   {@link GsonBuilder#registerTypeAdapter(Type, Object)}. </li>
   *   <li>The default Date format is same as {@link java.text.DateFormat#DEFAULT}. This format
   *   ignores the millisecond portion of the date during serialization. You can change
   *   this by invoking {@link GsonBuilder#setDateFormat(int)} or
   *   {@link GsonBuilder#setDateFormat(String)}. </li>
   *   <li>By default, Gson ignores the {@link com.google.gson.annotations.Expose} annotation.
   *   You can enable Gson to serialize/deserialize only those fields marked with this annotation
   *   through {@link GsonBuilder#excludeFieldsWithoutExposeAnnotation()}. </li>
   *   <li>By default, Gson ignores the {@link com.google.gson.annotations.Since} annotation. You
   *   can enable Gson to use this annotation through {@link GsonBuilder#setVersion(double)}.</li>
   *   <li>The default field naming policy for the output Json is same as in Java. So, a Java class
   *   field <code>versionNumber</code> will be output as <code>&quot;versionNumber&quot;</code> in
   *   Json. The same rules are applied for mapping incoming Json to the Java classes. You can
   *   change this policy through {@link GsonBuilder#setFieldNamingPolicy(FieldNamingPolicy)}.</li>
   *   <li>By default, Gson excludes <code>transient</code> or <code>static</code> fields from
   *   consideration for serialization and deserialization. You can change this behavior through
   *   {@link GsonBuilder#excludeFieldsWithModifiers(int...)}.</li>
   * </ul>
   */
  public Gson() { // 在创建Gson的时候，添加40多个adapter
    this(Excluder.DEFAULT, FieldNamingPolicy.IDENTITY,
        Collections.<Type, InstanceCreator<?>>emptyMap(), DEFAULT_SERIALIZE_NULLS,
        DEFAULT_COMPLEX_MAP_KEYS, DEFAULT_JSON_NON_EXECUTABLE, DEFAULT_ESCAPE_HTML,
        DEFAULT_PRETTY_PRINT, DEFAULT_LENIENT, DEFAULT_SPECIALIZE_FLOAT_VALUES,
        LongSerializationPolicy.DEFAULT, null, DateFormat.DEFAULT, DateFormat.DEFAULT,
        Collections.<TypeAdapterFactory>emptyList(), Collections.<TypeAdapterFactory>emptyList(),
        Collections.<TypeAdapterFactory>emptyList(), ToNumberPolicy.DOUBLE, ToNumberPolicy.LAZILY_PARSED_NUMBER);
  }

  Gson(Excluder excluder, FieldNamingStrategy fieldNamingStrategy,
      Map<Type, InstanceCreator<?>> instanceCreators, boolean serializeNulls,
      boolean complexMapKeySerialization, boolean generateNonExecutableGson, boolean htmlSafe,
      boolean prettyPrinting, boolean lenient, boolean serializeSpecialFloatingPointValues,
      LongSerializationPolicy longSerializationPolicy, String datePattern, int dateStyle,
      int timeStyle, List<TypeAdapterFactory> builderFactories,
      List<TypeAdapterFactory> builderHierarchyFactories,
      List<TypeAdapterFactory> factoriesToBeAdded,
          ToNumberStrategy objectToNumberStrategy, ToNumberStrategy numberToNumberStrategy) {

    // 排除器，在序列化对象的时候会根据使用者设置的规则排除一些数据
    // 排除策略需要使用者自行实现 ExclusionStrategy 接口来制定
    this.excluder = excluder;

    // fieldNamingStrategy 负责命名规则的确定(比如 大驼峰命名、小驼峰命名、下划线命名 等)
    // 选择不同的 fieldNamingStrategy 会在输出 json 字符串的时候把字段名称转成不同的命名形式
    // 此处的值可以直接选择 FieldNamingPolicy 枚举类中的已经存储的策略，也可以自行实现 FieldNamingStrategy 接口
    // 此处默认为 FieldNamingPolicy.IDENTITY，即不改变
    this.fieldNamingStrategy = fieldNamingStrategy;

    // instanceCreators 是一个用来存储实现了 InstanceCreator 接口的对象的 map
    // 每一个 InstanceCreator 的实现类用来反射获取一种特定类型的 bean 对象
    // InstanceCreator 在 Gson 中没有实现类，使用者可以自行定制
    // 此处为空 map
    this.instanceCreators = instanceCreators;

    // ConstructorConstructor 用来统一调度 instanceCreators
    this.constructorConstructor = new ConstructorConstructor(instanceCreators);

    // serializeNulls 是一个 boolean 类型的对象，用以表示是否支持空对象的序列化
    // 此处传入的是 DEFAULT_SERIALIZE_NULLS，值为 false，是一个定义在 Gson 中的常量
    this.serializeNulls = serializeNulls;

    // 将 Map 序列化的过程中，会存在一个问题，即 Map 的 key 值是一个复杂对象(java bean 等)
    // 如果 complexMapKeySerialization 设置为 false，则直接调用对象的 toString() 方法获取字符串
    // 设置为 true 的情况下会去尝试解析此对象，一般情况下要配合特定的 TypeAdapter 使用
    // 默认为 false
    this.complexMapKeySerialization = complexMapKeySerialization;

    // 是否要生成不可执行的 json
    // 默认为 false
    this.generateNonExecutableJson = generateNonExecutableGson;

    // 是否对 html 进行编码，即对部分符号进行转义(=、<、> 等)
    // 默认为 true
    this.htmlSafe = htmlSafe;

    // 在输出的时候格式化 json
    // 默认为 false
    this.prettyPrinting = prettyPrinting;


    this.lenient = lenient;

    // 用于支持 float 类型的特殊值，比如 Infinity(无穷大) 或 -Infinity(负无穷大) 等
    // 默认为 false
    this.serializeSpecialFloatingPointValues = serializeSpecialFloatingPointValues;

    // 设置对 long 类型的变量，是解析成字符串还是解析为 long 类型
    // 默认为解析成 long 类型
    this.longSerializationPolicy = longSerializationPolicy;

    // 以下三行用于设置日期格式和时间格式
    // datePattern 是日期和时间的字符串格式表达，在此处为 null
    // dateStyle 与 timeStyle 为日期和时间格式的编码
    // 以 int 常量形式存放在 java.text.DateFormat 中
    // 此处均为默认值
    // 需要注意的是默认情况下 Gson 的日期解析不太符合国人的习惯
    // TODO 注意这里
    this.datePattern = datePattern;
    this.dateStyle = dateStyle;
    this.timeStyle = timeStyle;

    this.builderFactories = builderFactories;
    this.builderHierarchyFactories = builderHierarchyFactories;
    this.objectToNumberStrategy = objectToNumberStrategy;
    this.numberToNumberStrategy = numberToNumberStrategy;

    // TypeAdapter 是一个接口，用于序列化和反序列化某种特定的类型
    // TypeAdapterFactory 是 TypeAdapter 的包装类
    List<TypeAdapterFactory> factories = new ArrayList<TypeAdapterFactory>();

    // built-in type adapters that cannot be overridden
    // TypeAdapters 是 TypeAdapter 和 TypeAdapterFactory 的通用工具类
    // 处理 JsonElement 类型对象的 TypeAdapterFactory
    // JsonElement 是 Gson 工具包中的一个类
    factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
    //处理 Object 类型对象的 TypeAdapterFactory
    factories.add(ObjectTypeAdapter.getFactory(objectToNumberStrategy));

    // the excluder must precede all adapters that handle user-defined types
    // excluder 是一个省略了类型的 TypeAdapterFactory
    // 根据官方注释，excluder 需要先于所有使用者自定义的 TypeAdapterFactory 去执行
    factories.add(excluder);

    // users' type adapters
    // 使用者自定义的 TypeAdapterFactory
    factories.addAll(factoriesToBeAdded);

    // type adapters for basic platform types
    // 处理 String 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.STRING_FACTORY);
    // 处理 Integer / int 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.INTEGER_FACTORY);
    // 处理 Boolean / boolean 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.BOOLEAN_FACTORY);
    // 处理 Byte / byte 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.BYTE_FACTORY);
    // 处理 Short / short 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.SHORT_FACTORY);
    // 处理 Long / long 类型对象的 TypeAdapterFactory
    TypeAdapter<Number> longAdapter = longAdapter(longSerializationPolicy);
    factories.add(TypeAdapters.newFactory(long.class, Long.class, longAdapter));
    // 处理 Double / double 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(double.class, Double.class,
            doubleAdapter(serializeSpecialFloatingPointValues)));
    // 处理 Number 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(float.class, Float.class,
            floatAdapter(serializeSpecialFloatingPointValues)));
    factories.add(NumberTypeAdapter.getFactory(numberToNumberStrategy));
    // 处理 AtomicInteger 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.ATOMIC_INTEGER_FACTORY);
    // 处理 AtomicBoolean 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.ATOMIC_BOOLEAN_FACTORY);
    // 处理 AtomicLong 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(AtomicLong.class, atomicLongAdapter(longAdapter)));
    // 处理 AtomicLongArray 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(AtomicLongArray.class, atomicLongArrayAdapter(longAdapter)));
    // 处理 AtomicIntegerArray 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.ATOMIC_INTEGER_ARRAY_FACTORY);
    // 处理 Character / char 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.CHARACTER_FACTORY);
    // 处理 StringBuilder 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.STRING_BUILDER_FACTORY);
    // 处理 StringBuffer 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.STRING_BUFFER_FACTORY);
    // 处理 BigDecimal 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(BigDecimal.class, TypeAdapters.BIG_DECIMAL));
    // 处理 BigInteger 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.newFactory(BigInteger.class, TypeAdapters.BIG_INTEGER));
    // 处理 URL 类型对象的 TypeAdapterFactory
    // java.net.URL
    factories.add(TypeAdapters.URL_FACTORY);
    // 处理 URI 类型对象的 TypeAdapterFactory
    // java.net.URI
    factories.add(TypeAdapters.URI_FACTORY);
    // 处理 UUID 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.UUID_FACTORY);
    // 处理 Currency 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.CURRENCY_FACTORY);
    // 处理 Locale 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.LOCALE_FACTORY);
    // 处理 InetAddress 类型对象的 TypeAdapterFactory
    // java.net.InetAddress
    factories.add(TypeAdapters.INET_ADDRESS_FACTORY);
    // 处理 BitSet 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.BIT_SET_FACTORY);
    // 处理 Date 类型对象的 TypeAdapterFactory
    // java.util.Date
    factories.add(DateTypeAdapter.FACTORY);
    //处理 Calendar 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.CALENDAR_FACTORY);

    if (SqlTypesSupport.SUPPORTS_SQL_TYPES) {
      factories.add(SqlTypesSupport.TIME_FACTORY);
      factories.add(SqlTypesSupport.DATE_FACTORY);
      factories.add(SqlTypesSupport.TIMESTAMP_FACTORY);
    }

    // 处理 Array 类型对象的 TypeAdapterFactory
    factories.add(ArrayTypeAdapter.FACTORY);
    // 处理 Class 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.CLASS_FACTORY);

    // type adapters for composite and user-defined types
    // 处理 Collection 类型对象的 TypeAdapterFactory
    factories.add(new CollectionTypeAdapterFactory(constructorConstructor));

    // 处理 Map 类型对象的 TypeAdapterFactory
    // 会受到 complexMapKeySerialization 的影响
    factories.add(new MapTypeAdapterFactory(constructorConstructor, complexMapKeySerialization));

    // 处理 JsonAdapter 类型对象的 TypeAdapterFactory
    // JsonAdapter 是一个 Gson 中的注解
    this.jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
    factories.add(jsonAdapterFactory);

    // 处理 Enum 类型对象的 TypeAdapterFactory
    factories.add(TypeAdapters.ENUM_FACTORY);

    // TODO 一般是这个对象
    // 反射分解对象的 TypeAdapterFactory
    factories.add(new ReflectiveTypeAdapterFactory(
        constructorConstructor, fieldNamingStrategy, excluder, jsonAdapterFactory));

    this.factories = Collections.unmodifiableList(factories);
  }

  /**
   * Returns a new GsonBuilder containing all custom factories and configuration used by the current
   * instance.
   *
   * @return a GsonBuilder instance.
   */
  public GsonBuilder newBuilder() {
    return new GsonBuilder(this);
  }

  public Excluder excluder() {
    return excluder;
  }

  public FieldNamingStrategy fieldNamingStrategy() {
    return fieldNamingStrategy;
  }

  public boolean serializeNulls() {
    return serializeNulls;
  }

  public boolean htmlSafe() {
    return htmlSafe;
  }

  private TypeAdapter<Number> doubleAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return TypeAdapters.DOUBLE;
    }
    return new TypeAdapter<Number>() {
      @Override public Double read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextDouble();
      }
      @Override public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        double doubleValue = value.doubleValue();
        checkValidFloatingPoint(doubleValue);
        out.value(value);
      }
    };
  }

  private TypeAdapter<Number> floatAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return TypeAdapters.FLOAT;
    }
    return new TypeAdapter<Number>() {
      @Override public Float read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return (float) in.nextDouble();
      }
      @Override public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        float floatValue = value.floatValue();
        checkValidFloatingPoint(floatValue);
        out.value(value);
      }
    };
  }

  static void checkValidFloatingPoint(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(value
          + " is not a valid double value as per JSON specification. To override this"
          + " behavior, use GsonBuilder.serializeSpecialFloatingPointValues() method.");
    }
  }

  private static TypeAdapter<Number> longAdapter(LongSerializationPolicy longSerializationPolicy) {
    if (longSerializationPolicy == LongSerializationPolicy.DEFAULT) {
      return TypeAdapters.LONG;
    }
    return new TypeAdapter<Number>() {
      @Override public Number read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextLong();
      }
      @Override public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        out.value(value.toString());
      }
    };
  }

  private static TypeAdapter<AtomicLong> atomicLongAdapter(final TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLong>() {
      @Override public void write(JsonWriter out, AtomicLong value) throws IOException {
        longAdapter.write(out, value.get());
      }
      @Override public AtomicLong read(JsonReader in) throws IOException {
        Number value = longAdapter.read(in);
        return new AtomicLong(value.longValue());
      }
    }.nullSafe();
  }

  private static TypeAdapter<AtomicLongArray> atomicLongArrayAdapter(final TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLongArray>() {
      @Override public void write(JsonWriter out, AtomicLongArray value) throws IOException {
        out.beginArray();
        for (int i = 0, length = value.length(); i < length; i++) {
          longAdapter.write(out, value.get(i));
        }
        out.endArray();
      }
      @Override public AtomicLongArray read(JsonReader in) throws IOException {
        List<Long> list = new ArrayList<Long>();
        in.beginArray();
        while (in.hasNext()) {
            long value = longAdapter.read(in).longValue();
            list.add(value);
        }
        in.endArray();
        int length = list.size();
        AtomicLongArray array = new AtomicLongArray(length);
        for (int i = 0; i < length; ++i) {
          array.set(i, list.get(i));
        }
        return array;
      }
    }.nullSafe();
  }

  /**
   * 返回某一个类的类型适配器
   * 这个方法获取适配器可能会出现循环引用的情况，需要避免。所以使用了一个ThreadLocal，并加了一些判断
   * Returns the type adapter for {@code} type.
   *
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
    TypeAdapter<?> cached = typeTokenCache.get(type == null ? NULL_KEY_SURROGATE : type);
    if (cached != null) {
      return (TypeAdapter<T>) cached;
    }

    Map<TypeToken<?>, FutureTypeAdapter<?>> threadCalls = calls.get();
    boolean requiresThreadLocalCleanup = false;
    if (threadCalls == null) {
      threadCalls = new HashMap<TypeToken<?>, FutureTypeAdapter<?>>();
      calls.set(threadCalls);
      requiresThreadLocalCleanup = true;
    }

    // the key and value type parameters always agree
    FutureTypeAdapter<T> ongoingCall = (FutureTypeAdapter<T>) threadCalls.get(type);
    if (ongoingCall != null) {
      return ongoingCall;
    }

    try {
      FutureTypeAdapter<T> call = new FutureTypeAdapter<T>();
      threadCalls.put(type, call);

      for (TypeAdapterFactory factory : factories) {
        // 这里在遍历的时候会创建一个候选类型适配器，之后
        TypeAdapter<T> candidate = factory.create(this, type); // factory = ReflectiveTypeAdapterFactory
        if (candidate != null) {
          call.setDelegate(candidate);
          typeTokenCache.put(type, candidate);
          return candidate;
        }
      }
      throw new IllegalArgumentException("GSON (" + GsonBuildConfig.VERSION + ") cannot handle " + type);
    } finally {
      threadCalls.remove(type);

      if (requiresThreadLocalCleanup) {
        calls.remove();
      }
    }
  }

  /**
   * This method is used to get an alternate type adapter for the specified type. This is used
   * to access a type adapter that is overridden by a {@link TypeAdapterFactory} that you
   * may have registered. This features is typically used when you want to register a type
   * adapter that does a little bit of work but then delegates further processing to the Gson
   * default type adapter. Here is an example:
   * <p>Let's say we want to write a type adapter that counts the number of objects being read
   *  from or written to JSON. We can achieve this by writing a type adapter factory that uses
   *  the <code>getDelegateAdapter</code> method:
   *  <pre> {@code
   *  class StatsTypeAdapterFactory implements TypeAdapterFactory {
   *    public int numReads = 0;
   *    public int numWrites = 0;
   *    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
   *      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
   *      return new TypeAdapter<T>() {
   *        public void write(JsonWriter out, T value) throws IOException {
   *          ++numWrites;
   *          delegate.write(out, value);
   *        }
   *        public T read(JsonReader in) throws IOException {
   *          ++numReads;
   *          return delegate.read(in);
   *        }
   *      };
   *    }
   *  }
   *  } </pre>
   *  This factory can now be used like this:
   *  <pre> {@code
   *  StatsTypeAdapterFactory stats = new StatsTypeAdapterFactory();
   *  Gson gson = new GsonBuilder().registerTypeAdapterFactory(stats).create();
   *  // Call gson.toJson() and fromJson methods on objects
   *  System.out.println("Num JSON reads" + stats.numReads);
   *  System.out.println("Num JSON writes" + stats.numWrites);
   *  }</pre>
   *  Note that this call will skip all factories registered before {@code skipPast}. In case of
   *  multiple TypeAdapterFactories registered it is up to the caller of this function to insure
   *  that the order of registration does not prevent this method from reaching a factory they
   *  would expect to reply from this call.
   *  Note that since you can not override type adapter factories for String and Java primitive
   *  types, our stats factory will not count the number of String or primitives that will be
   *  read or written.
   * @param skipPast The type adapter factory that needs to be skipped while searching for
   *   a matching type adapter. In most cases, you should just pass <i>this</i> (the type adapter
   *   factory from where {@link #getDelegateAdapter} method is being invoked).
   * @param type Type for which the delegate adapter is being searched for.
   *
   * @since 2.2
   */
  public <T> TypeAdapter<T> getDelegateAdapter(TypeAdapterFactory skipPast, TypeToken<T> type) {
    // Hack. If the skipPast factory isn't registered, assume the factory is being requested via
    // our @JsonAdapter annotation.
    if (!factories.contains(skipPast)) {
      skipPast = jsonAdapterFactory;
    }

    boolean skipPastFound = false;
    for (TypeAdapterFactory factory : factories) {
      if (!skipPastFound) {
        if (factory == skipPast) {
          skipPastFound = true;
        }
        continue;
      }

      TypeAdapter<T> candidate = factory.create(this, type);
      if (candidate != null) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("GSON cannot serialize " + type);
  }

  /**
   * Returns the type adapter for {@code} type.
   *
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  public <T> TypeAdapter<T> getAdapter(Class<T> type) {
    return getAdapter(TypeToken.get(type));
  }

  /**
   * This method serializes the specified object into its equivalent representation as a tree of
   * {@link JsonElement}s. This method should be used when the specified object is not a generic
   * type. This method uses {@link Class#getClass()} to get the type for the specified object, but
   * the {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJsonTree(Object, Type)} instead.
   *
   * @param src the object for which Json representation is to be created setting for Gson
   * @return Json representation of {@code src}.
   * @since 1.4
   */
  public JsonElement toJsonTree(Object src) {
    if (src == null) {
      return JsonNull.INSTANCE;
    }
    return toJsonTree(src, src.getClass());
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent representation as a tree of {@link JsonElement}s. This method must be used if the
   * specified object is a generic type. For non-generic objects, use {@link #toJsonTree(Object)}
   * instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link com.google.gson.reflect.TypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return Json representation of {@code src}
   * @since 1.4
   */
  public JsonElement toJsonTree(Object src, Type typeOfSrc) {
    JsonTreeWriter writer = new JsonTreeWriter();
    toJson(src, typeOfSrc, writer);
    return writer.get();
  }

  /**
   * This method serializes the specified object into its equivalent Json representation.
   * This method should be used when the specified object is not a generic type. This method uses
   * {@link Class#getClass()} to get the type for the specified object, but the
   * {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJson(Object, Type)} instead. If you want to write out the object to a
   * {@link Writer}, use {@link #toJson(Object, Appendable)} instead.
   *
   * @param src the object for which Json representation is to be created setting for Gson
   * @return Json representation of {@code src}.
   */
  public String toJson(Object src) {
    if (src == null) {
      return toJson(JsonNull.INSTANCE);
    }
    return toJson(src, src.getClass());
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent Json representation. This method must be used if the specified object is a generic
   * type. For non-generic objects, use {@link #toJson(Object)} instead. If you want to write out
   * the object to a {@link Appendable}, use {@link #toJson(Object, Type, Appendable)} instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link com.google.gson.reflect.TypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return Json representation of {@code src}
   */
  public String toJson(Object src, Type typeOfSrc) {
    StringWriter writer = new StringWriter();
    toJson(src, typeOfSrc, writer);
    return writer.toString();
  }

  /**
   * This method serializes the specified object into its equivalent Json representation.
   * This method should be used when the specified object is not a generic type. This method uses
   * {@link Class#getClass()} to get the type for the specified object, but the
   * {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJson(Object, Type, Appendable)} instead.
   *
   * @param src the object for which Json representation is to be created setting for Gson
   * @param writer Writer to which the Json representation needs to be written
   * @throws JsonIOException if there was a problem writing to the writer
   * @since 1.2
   */
  public void toJson(Object src, Appendable writer) throws JsonIOException {
    if (src != null) {
      toJson(src, src.getClass(), writer);
    } else {
      toJson(JsonNull.INSTANCE, writer);
    }
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent Json representation. This method must be used if the specified object is a generic
   * type. For non-generic objects, use {@link #toJson(Object, Appendable)} instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link com.google.gson.reflect.TypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @param writer Writer to which the Json representation of src needs to be written.
   * @throws JsonIOException if there was a problem writing to the writer
   * @since 1.2
   */
  public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
    try {
      JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
      toJson(src, typeOfSrc, jsonWriter);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }

  /**
   * Writes the JSON representation of {@code src} of type {@code typeOfSrc} to
   * {@code writer}.
   * @throws JsonIOException if there was a problem writing to the writer
   */
  @SuppressWarnings("unchecked")
  public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
    TypeAdapter<?> adapter = getAdapter(TypeToken.get(typeOfSrc));
    boolean oldLenient = writer.isLenient();
    writer.setLenient(true);
    boolean oldHtmlSafe = writer.isHtmlSafe();
    writer.setHtmlSafe(htmlSafe);
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setSerializeNulls(serializeNulls);
    try {
      ((TypeAdapter<Object>) adapter).write(writer, src);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      writer.setLenient(oldLenient);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }

  /**
   * Converts a tree of {@link JsonElement}s into its equivalent JSON representation.
   *
   * @param jsonElement root of a tree of {@link JsonElement}s
   * @return JSON String representation of the tree
   * @since 1.4
   */
  public String toJson(JsonElement jsonElement) {
    StringWriter writer = new StringWriter();
    toJson(jsonElement, writer);
    return writer.toString();
  }

  /**
   * Writes out the equivalent JSON for a tree of {@link JsonElement}s.
   *
   * @param jsonElement root of a tree of {@link JsonElement}s
   * @param writer Writer to which the Json representation needs to be written
   * @throws JsonIOException if there was a problem writing to the writer
   * @since 1.4
   */
  public void toJson(JsonElement jsonElement, Appendable writer) throws JsonIOException {
    try {
      JsonWriter jsonWriter = newJsonWriter(Streams.writerForAppendable(writer));
      toJson(jsonElement, jsonWriter);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }

  /**
   * Returns a new JSON writer configured for the settings on this Gson instance.
   */
  public JsonWriter newJsonWriter(Writer writer) throws IOException {
    if (generateNonExecutableJson) {
      writer.write(JSON_NON_EXECUTABLE_PREFIX);
    }
    JsonWriter jsonWriter = new JsonWriter(writer);
    if (prettyPrinting) {
      jsonWriter.setIndent("  ");
    }
    jsonWriter.setSerializeNulls(serializeNulls);
    return jsonWriter;
  }

  /**
   * Returns a new JSON reader configured for the settings on this Gson instance.
   * <p>
   * 第一次进来的时候主要就是读取了字节流，存在成员变量in里面
   * 然后就是设置了JSON语法规则为false
   */
  public JsonReader newJsonReader(Reader reader) {
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setLenient(lenient); // true:宽松的JSON语法规则
    return jsonReader;
  }

  /**
   * Writes the JSON for {@code jsonElement} to {@code writer}.
   * @throws JsonIOException if there was a problem writing to the writer
   */
  public void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIOException {
    boolean oldLenient = writer.isLenient();
    writer.setLenient(true);
    boolean oldHtmlSafe = writer.isHtmlSafe();
    writer.setHtmlSafe(htmlSafe);
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setSerializeNulls(serializeNulls);
    try {
      Streams.write(jsonElement, writer);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      writer.setLenient(oldLenient);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }

  /**
   * This method deserializes the specified Json into an object of the specified class. It is not
   * suitable to use if the specified class is a generic type since it will not have the generic
   * type information because of the Type Erasure feature of Java. Therefore, this method should not
   * be used if the desired type is a generic type. Note that this method works fine if the any of
   * the fields of the specified object are generics, just the object itself should not be a
   * generic type. For the cases when the object is of generic type, invoke
   * {@link #fromJson(String, Type)}. If you have the Json in a {@link Reader} instead of
   * a String, use {@link #fromJson(Reader, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the string from which the object is to be deserialized
   * @param classOfT the class of T
   * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   * classOfT
   */
  public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
    Object object = fromJson(json, (Type) classOfT);
    return Primitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the specified Json into an object of the specified type. This method
   * is useful if the specified object is a generic type. For non-generic objects, use
   * {@link #fromJson(String, Class)} instead. If you have the Json in a {@link Reader} instead of
   * a String, use {@link #fromJson(Reader, Type)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the string from which the object is to be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws JsonParseException if json is not a valid representation for an object of type typeOfT
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    // StringReader 是一个 jdk 中存在的 String 和 Reader 的关联封装类
    StringReader reader = new StringReader(json); // StringBuilder的作用
    // 主体功能实现方法
    T target = (T) fromJson(reader, typeOfT);
    // 返回一个指定泛型的对象
    return target;
  }

  /**
   * This method deserializes the Json read from the specified reader into an object of the
   * specified class. It is not suitable to use if the specified class is a generic type since it
   * will not have the generic type information because of the Type Erasure feature of Java.
   * Therefore, this method should not be used if the desired type is a generic type. Note that
   * this method works fine if the any of the fields of the specified object are generics, just the
   * object itself should not be a generic type. For the cases when the object is of generic type,
   * invoke {@link #fromJson(Reader, Type)}. If you have the Json in a String form instead of a
   * {@link Reader}, use {@link #fromJson(String, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the reader producing the Json from which the object is to be deserialized.
   * @param classOfT the class of T
   * @return an object of type T from the string. Returns {@code null} if {@code json} is at EOF.
   * @throws JsonIOException if there was a problem reading from the Reader
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   * @since 1.2
   */
  public <T> T fromJson(Reader json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
    JsonReader jsonReader = newJsonReader(json);
    Object object = fromJson(jsonReader, classOfT);
    assertFullConsumption(object, jsonReader);
    return Primitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the Json read from the specified reader into an object of the
   * specified type. This method is useful if the specified object is a generic type. For
   * non-generic objects, use {@link #fromJson(Reader, Class)} instead. If you have the Json in a
   * String form instead of a {@link Reader}, use {@link #fromJson(String, Type)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the reader producing Json from which the object is to be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the json. Returns {@code null} if {@code json} is at EOF.
   * @throws JsonIOException if there was a problem reading from the Reader
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   * @since 1.2
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    JsonReader jsonReader = newJsonReader(json); // 在使用JsonReader读取的时候做了什么事情
    T object = (T) fromJson(jsonReader, typeOfT); // 将包装后的JsonReader转为对象
    assertFullConsumption(object, jsonReader);
    return object;
  }

  private static void assertFullConsumption(Object obj, JsonReader reader) {
    try {
      if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonIOException("JSON document was not fully consumed.");
      }
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    }
  }

  /**
   * Reads the next JSON value from {@code reader} and convert it to an object
   * of type {@code typeOfT}. Returns {@code null}, if the {@code reader} is at EOF.
   * Since Type is not parameterized by T, this method is type unsafe and should be used carefully
   *
   * <p>
   *     从 reader 读取下一个 JSON 值并将其转换为 typeOfT 类型的对象。这里是读取的入口
   * </p>
   * @throws JsonIOException if there was a problem writing to the Reader
   * @throws JsonSyntaxException if json is not a valid representation for an object of type
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    boolean isEmpty = true;
    boolean oldLenient = reader.isLenient();
    // 默认格式是要求严格一致的
    reader.setLenient(true);
    try {
      reader.peek(); // 这个方法的作用？这里就是把json字符串的第一个字符"{"consume
      isEmpty = false;
      TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT); // TypeToken 本质上是 Class 的增强封装类；typeOfT的值为：com.google.gson.User
      TypeAdapter<T> typeAdapter = getAdapter(typeToken); // 在这里获取适配器Adapter
      T object = typeAdapter.read(reader);
      return object;
    } catch (EOFException e) {
      /*
       * For compatibility with JSON 1.5 and earlier, we return null for empty
       * documents instead of throwing.
       */
      if (isEmpty) {
        return null;
      }
      throw new JsonSyntaxException(e);
    } catch (IllegalStateException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      // TODO(inder): Figure out whether it is indeed right to rethrow this as JsonSyntaxException
      throw new JsonSyntaxException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      reader.setLenient(oldLenient);
    }
  }

  /**
   * This method deserializes the Json read from the specified parse tree into an object of the
   * specified type. It is not suitable to use if the specified class is a generic type since it
   * will not have the generic type information because of the Type Erasure feature of Java.
   * Therefore, this method should not be used if the desired type is a generic type. Note that
   * this method works fine if the any of the fields of the specified object are generics, just the
   * object itself should not be a generic type. For the cases when the object is of generic type,
   * invoke {@link #fromJson(JsonElement, Type)}.
   * @param <T> the type of the desired object
   * @param json the root of the parse tree of {@link JsonElement}s from which the object is to
   * be deserialized
   * @param classOfT The class of T
   * @return an object of type T from the json. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
   * @since 1.3
   */
  public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
    Object object = fromJson(json, (Type) classOfT);
    return Primitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the Json read from the specified parse tree into an object of the
   * specified type. This method is useful if the specified object is a generic type. For
   * non-generic objects, use {@link #fromJson(JsonElement, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the root of the parse tree of {@link JsonElement}s from which the object is to
   * be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the json. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
   * @since 1.3
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
    if (json == null) {
      return null;
    }
    return (T) fromJson(new JsonTreeReader(json), typeOfT);
  }

  static class FutureTypeAdapter<T> extends TypeAdapter<T> {
    private TypeAdapter<T> delegate;

    public void setDelegate(TypeAdapter<T> typeAdapter) {
      if (delegate != null) {
        throw new AssertionError();
      }
      delegate = typeAdapter;
    }

    @Override public T read(JsonReader in) throws IOException {
      if (delegate == null) {
        throw new IllegalStateException();
      }
      return delegate.read(in);
    }

    @Override public void write(JsonWriter out, T value) throws IOException {
      if (delegate == null) {
        throw new IllegalStateException();
      }
      delegate.write(out, value);
    }
  }

  @Override
  public String toString() {
    return new StringBuilder("{serializeNulls:")
        .append(serializeNulls)
        .append(",factories:").append(factories)
        .append(",instanceCreators:").append(constructorConstructor)
        .append("}")
        .toString();
  }
}
