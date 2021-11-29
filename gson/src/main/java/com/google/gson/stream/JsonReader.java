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

import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.bind.JsonTreeReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * Reads a JSON (<a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>)
 * encoded value as a stream of tokens. This stream includes both literal
 * values (strings, numbers, booleans, and nulls) as well as the begin and
 * end delimiters of objects and arrays. The tokens are traversed in
 * depth-first order, the same order that they appear in the JSON document.
 * Within JSON objects, name/value pairs are represented by a single token.
 *
 * <p>读取 JSON (RFC 7159) 编码值作为令牌流。
 * <p>该流包括字面值（字符串、数字、布尔值和空值）以及对象和数组的开始和结束分隔符。
 * <p>令牌以深度优先顺序遍历，与它们在 JSON 文档中出现的顺序相同。
 * <p>在 JSON 对象中，名称/值对由单个标记表示。
 * <p>
 *
 * <h3>Parsing JSON</h3>
 * To create a recursive descent parser for your own JSON streams, first create
 * an entry point method that creates a {@code JsonReader}.
 * <p>
 * <p>要为你自己的 JSON 流创建递归解析器，首先需要创建一个用于创建 JsonReader 的入口方法。
 *
 * <p>Next, create handler methods for each structure in your JSON text. You'll
 * need a method for each object type and for each array type.
 *
 * <p>接着，为 JSON 文本中的每个结构创建处理方法。
 * <p>对于每种对象类型和每种数组类型，都需要一个方法去处理。
 *
 * <ul>
 *   <li>Within <strong>array handling</strong> methods, first call {@link
 *       #beginArray} to consume the array's opening bracket. Then create a
 *       while loop that accumulates values, terminating when {@link #hasNext}
 *       is false. Finally, read the array's closing bracket by calling {@link
 *       #endArray}.
 *   <li>Within <strong>object handling</strong> methods, first call {@link
 *       #beginObject} to consume the object's opening brace. Then create a
 *       while loop that assigns values to local variables based on their name.
 *       This loop should terminate when {@link #hasNext} is false. Finally,
 *       read the object's closing brace by calling {@link #endObject}.
 * </ul>
 *
 * <ul>
 *     <li>
 *         读取一个数组时，首先调用 beginArray 来读取数组的左括号。
 *         然后在 while 中循环读取数组值，当 hasNext 为 false 时终止。
 *         最后，通过调用 endArray 读取数组的右括号。
 *     </li>
 *     <li>
 *         在对象处理方法中，首先调用 beginObject 来使用对象的左大括号。
 *         然后创建一个 while 循环，根据名称为局部变量赋值。
 *         当 hasNext 为 false 时，此循环应终止。
 *         最后，通过调用 endObject 读取对象的右大括号。
 *     </li>
 * </ul>
 *
 * <p>When a nested object or array is encountered, delegate to the
 * corresponding handler method.
 *
 * <p>当遇到嵌套对象或数组时，委托给相应的处理程序方法。
 *
 * <p>When an unknown name is encountered, strict parsers should fail with an
 * exception. Lenient parsers should call {@link #skipValue()} to recursively
 * skip the value's nested tokens, which may otherwise conflict.
 *
 * <p>当遇到未知名称时，严格解析器应该会失败并抛出异常。宽松的解析器应该调用 skipValue() 递归地跳过值的嵌套标记，否则可能会发生冲突。
 *
 * <p>If a value may be null, you should first check using {@link #peek()}.
 * Null literals can be consumed using either {@link #nextNull()} or {@link
 * #skipValue()}.
 *
 * <p>如果一个值可能为空，您应该首先使用 peek() 检查。 可以使用 nextNull() 或 skipValue() 跳过空的值。
 *
 * <h3>Example</h3>
 * Suppose we'd like to parse a stream of messages such as the following: <pre> {@code
 * [
 *   {
 *     "id": 912345678901,
 *     "text": "How do I read a JSON stream in Java?",
 *     "geo": null,
 *     "user": {
 *       "name": "json_newb",
 *       "followers_count": 41
 *      }
 *   },
 *   {
 *     "id": 912345678902,
 *     "text": "@json_newb just use JsonReader!",
 *     "geo": [50.454722, -104.606667],
 *     "user": {
 *       "name": "jesse",
 *       "followers_count": 2
 *     }
 *   }
 * ]}</pre>
 * This code implements the parser for the above structure: <pre>   {@code
 *
 *   public List<Message> readJsonStream(InputStream in) throws IOException {
 *     JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
 *     try {
 *       return readMessagesArray(reader);
 *     } finally {
 *       reader.close();
 *     }
 *   }
 *
 *   public List<Message> readMessagesArray(JsonReader reader) throws IOException {
 *     List<Message> messages = new ArrayList<Message>();
 *
 *     reader.beginArray();
 *     while (reader.hasNext()) {
 *       messages.add(readMessage(reader));
 *     }
 *     reader.endArray();
 *     return messages;
 *   }
 *
 *   public Message readMessage(JsonReader reader) throws IOException {
 *     long id = -1;
 *     String text = null;
 *     User user = null;
 *     List<Double> geo = null;
 *
 *     reader.beginObject();
 *     while (reader.hasNext()) {
 *       String name = reader.nextName();
 *       if (name.equals("id")) {
 *         id = reader.nextLong();
 *       } else if (name.equals("text")) {
 *         text = reader.nextString();
 *       } else if (name.equals("geo") && reader.peek() != JsonToken.NULL) {
 *         geo = readDoublesArray(reader);
 *       } else if (name.equals("user")) {
 *         user = readUser(reader);
 *       } else {
 *         reader.skipValue();
 *       }
 *     }
 *     reader.endObject();
 *     return new Message(id, text, user, geo);
 *   }
 *
 *   public List<Double> readDoublesArray(JsonReader reader) throws IOException {
 *     List<Double> doubles = new ArrayList<Double>();
 *
 *     reader.beginArray();
 *     while (reader.hasNext()) {
 *       doubles.add(reader.nextDouble());
 *     }
 *     reader.endArray();
 *     return doubles;
 *   }
 *
 *   public User readUser(JsonReader reader) throws IOException {
 *     String username = null;
 *     int followersCount = -1;
 *
 *     reader.beginObject();
 *     while (reader.hasNext()) {
 *       String name = reader.nextName();
 *       if (name.equals("name")) {
 *         username = reader.nextString();
 *       } else if (name.equals("followers_count")) {
 *         followersCount = reader.nextInt();
 *       } else {
 *         reader.skipValue();
 *       }
 *     }
 *     reader.endObject();
 *     return new User(username, followersCount);
 *   }}</pre>
 *
 * <h3>Number Handling</h3>
 * This reader permits numeric values to be read as strings and string values to
 * be read as numbers. For example, both elements of the JSON array {@code
 * [1, "1"]} may be read using either {@link #nextInt} or {@link #nextString}.
 * This behavior is intended to prevent lossy numeric conversions: double is
 * JavaScript's only numeric type and very large values like {@code
 * 9007199254740993} cannot be represented exactly on that platform. To minimize
 * precision loss, extremely large values should be written and read as strings
 * in JSON.
 *
 * <a id="nonexecuteprefix"/><h3>Non-Execute Prefix</h3>
 * Web servers that serve private data using JSON may be vulnerable to <a
 * href="http://en.wikipedia.org/wiki/JSON#Cross-site_request_forgery">Cross-site
 * request forgery</a> attacks. In such an attack, a malicious site gains access
 * to a private JSON file by executing it with an HTML {@code <script>} tag.
 *
 * <p>Prefixing JSON files with <code>")]}'\n"</code> makes them non-executable
 * by {@code <script>} tags, disarming the attack. Since the prefix is malformed
 * JSON, strict parsing fails when it is encountered. This class permits the
 * non-execute prefix when {@link #setLenient(boolean) lenient parsing} is
 * enabled.
 *
 * <p>Each {@code JsonReader} may be used to read a single JSON stream. Instances
 * of this class are not thread safe.
 *
 * @author Jesse Wilson
 * @since 1.6
 */
public class JsonReader implements Closeable {
  private static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;

  private static final int PEEKED_NONE = 0;
  private static final int PEEKED_BEGIN_OBJECT = 1;
  private static final int PEEKED_END_OBJECT = 2;
  private static final int PEEKED_BEGIN_ARRAY = 3;
  private static final int PEEKED_END_ARRAY = 4;
  private static final int PEEKED_TRUE = 5;
  private static final int PEEKED_FALSE = 6;
  private static final int PEEKED_NULL = 7;
  private static final int PEEKED_SINGLE_QUOTED = 8;
  private static final int PEEKED_DOUBLE_QUOTED = 9;
  private static final int PEEKED_UNQUOTED = 10;
  /** When this is returned, the string value is stored in peekedString. */
  private static final int PEEKED_BUFFERED = 11;
  private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
  private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
  private static final int PEEKED_UNQUOTED_NAME = 14;
  /** When this is returned, the integer value is stored in peekedLong. */
  private static final int PEEKED_LONG = 15;
  private static final int PEEKED_NUMBER = 16;
  private static final int PEEKED_EOF = 17;

  /* State machine when parsing numbers */
  private static final int NUMBER_CHAR_NONE = 0;
  private static final int NUMBER_CHAR_SIGN = 1;
  private static final int NUMBER_CHAR_DIGIT = 2;
  private static final int NUMBER_CHAR_DECIMAL = 3;
  private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
  private static final int NUMBER_CHAR_EXP_E = 5;
  private static final int NUMBER_CHAR_EXP_SIGN = 6;
  private static final int NUMBER_CHAR_EXP_DIGIT = 7;

  /**
   * The input JSON.
   * 输入的json
   */
  private final Reader in;

  /** True to accept non-spec compliant JSON */
  private boolean lenient = false;

  /**
   * Use a manual buffer to easily read and unread upcoming characters, and
   * also so we can create strings without an intermediate StringBuilder.
   * We decode literals directly out of this buffer, so it must be at least as
   * long as the longest token that can be reported as a number.
   *
   * 使用手动缓冲区轻松读取和跳过即将到来的字符，并且我们也可以不使用 StringBuilder 创建字符串。
   * 我们直接从该缓冲区中解码文字，因此它必须至少与可以报告为数字的最长标记一样长。
   */
  private final char[] buffer = new char[1024];
  private int pos = 0;
  private int limit = 0;

  private int lineNumber = 0;
  private int lineStart = 0;

  int peeked = PEEKED_NONE;

  /**
   * A peeked value that was composed entirely of digits with an optional
   * leading dash. Positive values may not have a leading 0.
   *
   * 一个完全由数字组成的偷看值，带有一个可选的领先的破折号。
   * 正值可能没有前导 0。
   */
  private long peekedLong;

  /**
   * The number of characters in a peeked number literal. Increment 'pos' by
   * this after reading a number.
   */
  private int peekedNumberLength;

  /**
   * A peeked string that should be parsed on the next double, long or string.
   * This is populated before a numeric value is parsed and used if that parsing
   * fails.
   */
  private String peekedString;

  /*
   * The nesting stack. Using a manual array rather than an ArrayList saves 20%.
   * 嵌套堆栈。 使用手动数组而不是 ArrayList 可节省20%
   */
  private int[] stack = new int[32];
  private int stackSize = 0;
  {
    stack[stackSize++] = JsonScope.EMPTY_DOCUMENT;
  }

  /*
   * The path members. It corresponds directly to stack: At indices where the
   * stack contains an object (EMPTY_OBJECT, DANGLING_NAME or NONEMPTY_OBJECT),
   * pathNames contains the name at this scope. Where it contains an array
   * (EMPTY_ARRAY, NONEMPTY_ARRAY) pathIndices contains the current index in
   * that array. Otherwise the value is undefined, and we take advantage of that
   * by incrementing pathIndices when doing so isn't useful.
   */
  private String[] pathNames = new String[32];
  private int[] pathIndices = new int[32];

  /**
   * Creates a new instance that reads a JSON-encoded stream from {@code in}.
   */
  public JsonReader(Reader in) {
    if (in == null) {
      throw new NullPointerException("in == null");
    }
    this.in = in; // 然后就是存储了字节流
  }

  /**
   * Configure this parser to be liberal in what it accepts. By default,
   * this parser is strict and only accepts JSON as specified by <a
   * href="http://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>. Setting the
   * parser to lenient causes it to ignore the following syntax errors:
   *
   * <ul>
   *   <li>Streams that start with the <a href="#nonexecuteprefix">non-execute
   *       prefix</a>, <code>")]}'\n"</code>.
   *   <li>Streams that include multiple top-level values. With strict parsing,
   *       each stream must contain exactly one top-level value.
   *   <li>Top-level values of any type. With strict parsing, the top-level
   *       value must be an object or an array.
   *   <li>Numbers may be {@link Double#isNaN() NaNs} or {@link
   *       Double#isInfinite() infinities}.
   *   <li>End of line comments starting with {@code //} or {@code #} and
   *       ending with a newline character.
   *   <li>C-style comments starting with {@code /*} and ending with
   *       {@code *}{@code /}. Such comments may not be nested.
   *   <li>Names that are unquoted or {@code 'single quoted'}.
   *   <li>Strings that are unquoted or {@code 'single quoted'}.
   *   <li>Array elements separated by {@code ;} instead of {@code ,}.
   *   <li>Unnecessary array separators. These are interpreted as if null
   *       was the omitted value.
   *   <li>Names and values separated by {@code =} or {@code =>} instead of
   *       {@code :}.
   *   <li>Name/value pairs separated by {@code ;} instead of {@code ,}.
   * </ul>
   *
   * 这里就是设置解析是不是宽松的，默认的情况下，这里是true的，也就是格式严格一致的：
   * <ul>
   *    <li>以非执行前缀 ")]}'\n" 开头的流。
   *    <li>包含多个顶级值的流。使用严格的解析，每个流必须只包含一个顶级值。
   *    <li>任何类型的顶级值。使用严格解析，顶级值必须是对象或数组。
   *    <li>数字可以是 NaN 或无穷大。
   *    <li>以 // 或 # 开头并以换行符结尾的行尾注释。
   *    <li>以 {@code /*} 开头并以 {@code *}{@code /} 结尾的 C风格注释。此类注释不得嵌套。
   *    <li>不带引号或“单引号”的名称。
   *    <li>未加引号或“单引号”的字符串。
   *    <li>由;分隔的数组元素代替 ，。
   *    <li>不必要的数组分隔符。这些被解释为好像 null 是省略的值。
   *    <li>名称和值由 =或 =>而不是 :分隔。
   *    <li>以;分隔的名称/值对代替 ，。
   * </ul>
   */
  public final void setLenient(boolean lenient) {
    this.lenient = lenient;
  }

  /**
   * Returns true if this parser is liberal in what it accepts.
   */
  public final boolean isLenient() {
    return lenient;
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * beginning of a new array.
   */
  public void beginArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_ARRAY) {
      push(JsonScope.EMPTY_ARRAY);
      pathIndices[stackSize - 1] = 0;
      peeked = PEEKED_NONE;
    } else {
      throw new IllegalStateException("Expected BEGIN_ARRAY but was " + peek() + locationString());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * end of the current array.
   */
  public void endArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_ARRAY) {
      stackSize--;
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw new IllegalStateException("Expected END_ARRAY but was " + peek() + locationString());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * beginning of a new object.
   *
   * <p>consume JSON 流的下一个 token 并声明它是新对象的开始。
   */
  public void beginObject() throws IOException {
    int p = peeked;
    // 初始化时 peeked = PEEKED_NONE
    // 在 doPeek() 方法中会修改成 PEEKED_BEGIN_OBJECT，即开始一个 Object 的序列化
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_OBJECT) { // 开始一个对象
      // push(...) 方法会检查 stack 数组的容积，适时进行扩容，并把传入的指令存放到数组中
      // 此处将 EMPTY_OBJECT 指令存入到 stack 中
      push(JsonScope.EMPTY_OBJECT);
      //将 peeked 状态初始化
      peeked = PEEKED_NONE; // 在这里peek又变为0了
    } else {
      throw new IllegalStateException("Expected BEGIN_OBJECT but was " + peek() + locationString());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * end of the current object.
   */
  public void endObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_OBJECT) {
      stackSize--;
      pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw new IllegalStateException("Expected END_OBJECT but was " + peek() + locationString());
    }
  }

  /**
   * Returns true if the current array or object has another element.
   */
  public boolean hasNext() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY;
  }

  /**
   * Returns the type of the next token without consuming it.
   * <p>
   * 返回下一个的JsonToken
   */
  public JsonToken peek() throws IOException {
    int p = peeked; // 来到这里的时候p=0
    if (p == PEEKED_NONE) {
      p = doPeek(); // 读取完"{"后，p=1
    }

    switch (p) {
    case PEEKED_BEGIN_OBJECT:
      return JsonToken.BEGIN_OBJECT; // 第一次读取来到这里，返回这个JsonToken
    case PEEKED_END_OBJECT:
      return JsonToken.END_OBJECT;
    case PEEKED_BEGIN_ARRAY:
      return JsonToken.BEGIN_ARRAY;
    case PEEKED_END_ARRAY:
      return JsonToken.END_ARRAY;
    case PEEKED_SINGLE_QUOTED_NAME:
    case PEEKED_DOUBLE_QUOTED_NAME:
    case PEEKED_UNQUOTED_NAME:
      return JsonToken.NAME;
    case PEEKED_TRUE:
    case PEEKED_FALSE:
      return JsonToken.BOOLEAN;
    case PEEKED_NULL:
      return JsonToken.NULL;
    case PEEKED_SINGLE_QUOTED:
    case PEEKED_DOUBLE_QUOTED:
    case PEEKED_UNQUOTED:
    case PEEKED_BUFFERED:
      return JsonToken.STRING; // name对应的value来到这里
    case PEEKED_LONG:
    case PEEKED_NUMBER:
      return JsonToken.NUMBER;
    case PEEKED_EOF:
      return JsonToken.END_DOCUMENT;
    default:
      throw new AssertionError();
    }
  }

  /**
   * 在这里真正的处理返回数据
   */
  int doPeek() throws IOException {
    // stack 是一个定义在 JsonReader 中的 int 数组，作为 JsonReader 的指令集存在，用于控制变量 peeked 的状态
    // 在 JsonReader 初始化的时候会将 stack 的第一个元素变成6，其余均为0
    // 6的意思根据官方注释为 "No object or array has been started"(还没开始读取对象或列表)
    // 6作为常量保存在 JsonScope 中，JsonScope 中还保存了很多代表指令的常量，下列会用到
    // stackSize 是 stack 的有效元素计数器，初始化时 stackSize = 1，即只有第一个元素是有效的
    int peekStack = stack[stackSize - 1];

    //JsonScope.EMPTY_ARRAY = 1
    if (peekStack == JsonScope.EMPTY_ARRAY) {
      //JsonScope.NONEMPTY_ARRAY = 2
      stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
    } else if (peekStack == JsonScope.NONEMPTY_ARRAY) {
      // Look for a comma before the next element.
      // 在第一次调用 nextNonWhitespace(true) 方法的时候，json 字符串会被转存为一个 char 数组
      // 该方法以 int 值的形式返回下一个要解析的 char 对象
      int c = nextNonWhitespace(true);
      switch (c) {
      case ']':
        //peeked 是 JsonReader 中最重要的用来状态控制的 int 变量
        //peeked 和 stack 会协同控制 JsonReader 的逻辑行为
        return peeked = PEEKED_END_ARRAY;
      case ';':
        // 检查标准协议选项，json 标准中的符号没有分号
        // 所以在 lenient = false 的时候就会报错
        checkLenient(); // fall-through
      case ',':
        break;
      default:
        throw syntaxError("Unterminated array");
      }
      //JsonScope.EMPTY_OBJECT = 3，JsonScope.NONEMPTY_OBJECT = 5
    } else if (peekStack == JsonScope.EMPTY_OBJECT || peekStack == JsonScope.NONEMPTY_OBJECT) {
      //JsonScope.DANGLING_NAME = 4
      stack[stackSize - 1] = JsonScope.DANGLING_NAME;
      // Look for a comma before the next element.
      if (peekStack == JsonScope.NONEMPTY_OBJECT) {
        int c = nextNonWhitespace(true);
        switch (c) {
        case '}':
          return peeked = PEEKED_END_OBJECT;
        case ';':
          checkLenient(); // fall-through
        case ',':
          break;
        default:
          throw syntaxError("Unterminated object");
        }
      }

      // 上面就是判断这些符号的 ："," ";" "}"（bushi

      int c = nextNonWhitespace(true);
      switch (c) {
      case '"':
        return peeked = PEEKED_DOUBLE_QUOTED_NAME;
      case '\'':
        checkLenient();
        return peeked = PEEKED_SINGLE_QUOTED_NAME;
      case '}':
        if (peekStack != JsonScope.NONEMPTY_OBJECT) {
          return peeked = PEEKED_END_OBJECT;
        } else {
          throw syntaxError("Expected name");
        }
      default:
        checkLenient();
        pos--; // Don't consume the first character in an unquoted string.
        if (isLiteral((char) c)) {
          return peeked = PEEKED_UNQUOTED_NAME;
        } else {
          throw syntaxError("Expected name");
        }
      }
    } else if (peekStack == JsonScope.DANGLING_NAME) {
      stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
      // Look for a colon before the value.
      int c = nextNonWhitespace(true);
      switch (c) {
      case ':': // 读取到冒号了，break
        break;
      case '=':
        checkLenient();
        // buffer 是储存 json 字符串的 char 数组
        // pos 是已经读取到字符的数量指针
        // limit 是 buffer 的可用部分的总长
        if ((pos < limit || fillBuffer(1)) && buffer[pos] == '>') {
          pos++;
        }
        break;
      default:
        throw syntaxError("Expected ':'");
      }
      // 初始化的peekStack是6，也就是JsonScope.EMPTY_DOCUMENT，会在这个逻辑里面，读取字节流
      // JsonScope.EMPTY_DOCUMENT = 6
      // 第一次进入方法的时候，会进入这个 if 语句中
    } else if (peekStack == JsonScope.EMPTY_DOCUMENT) { // 第一步————初始读取一个json字符串的时候，会来到到这里
      if (lenient) {
        consumeNonExecutePrefix(); // 在这个方法里面，读取json流保存到char类型的buffer数组里面
      }
      // 赋值JsonScope.NONEMPTY_DOCUMENT = 7，表示：Json字符串中有一个顶级对象或者数组
      stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
    } else if (peekStack == JsonScope.NONEMPTY_DOCUMENT) {
      int c = nextNonWhitespace(false);
      if (c == -1) {
        return peeked = PEEKED_EOF;
      } else {
        checkLenient();
        System.out.println("repository");
        pos--;
      }
    } else if (peekStack == JsonScope.CLOSED) {
      throw new IllegalStateException("JsonReader is closed");
    }

    // 到这里，上面的逻辑判断结束

    // 在这里获取到了下一个要解析的 char 的 int 值
    // name读完之后，在前面读完冒号：break，接着读取name对应的value
    int c = nextNonWhitespace(true); // 前面将json流保存到buffer数组后，又来到这里获取第一个字符
    switch (c) {
    case ']':
      if (peekStack == JsonScope.EMPTY_ARRAY) {
        return peeked = PEEKED_END_ARRAY;
      }
      // fall-through to handle ",]"
    case ';':
    case ',':
      // In lenient mode, a 0-length literal in an array means 'null'.
      if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
        checkLenient();
        pos--;
        return peeked = PEEKED_NULL;
      } else {
        throw syntaxError("Unexpected value");
      }
    case '\'': // 读冒号的下一个，返回来的是"\'"
      checkLenient();
      return peeked = PEEKED_SINGLE_QUOTED; // name对应的value回来到这里，8
    case '"':
      return peeked = PEEKED_DOUBLE_QUOTED;
    case '[':
      return peeked = PEEKED_BEGIN_ARRAY;
    case '{':
      return peeked = PEEKED_BEGIN_OBJECT; // 1 读取的第一个是{，来到这里，返回这个token
    default:
      pos--; // Don't consume the first character in a literal value.
    }

    // 这个方法就是处理value值的大小写的
    // peekKeyword() 方法会从 buffer 数组里获取下一个 char
    // 然后根据这个字符判断下一个要处理的字符串是不是 true、false、null 等特殊字符
    // 如果不是，会返回 result = PEEKED_NONE
    int result = peekKeyword();
    if (result != PEEKED_NONE) {
      // 不等于 PEEKED_NONE，证明下一个确实是特殊字符
      return result;
    }

    // peekNumber() 方法和上述 peekKeyword() 方法很类似
    // 用于判断下一个要处理的字符串是否是数字
    result = peekNumber();
    if (result != PEEKED_NONE) {
      return result;
    }

    // isLiteral(buffer[pos]) 用于判断下一个字符是否是特殊符
    // 比如 换行符、井号、括号 等
    // 如果是 换行符 的话这里就会抛出错误
    if (!isLiteral(buffer[pos])) {
      throw syntaxError("Expected value");
    }

    checkLenient();
    return peeked = PEEKED_UNQUOTED;
  }

  private int peekKeyword() throws IOException {
    // Figure out which keyword we're matching against by its first character.
    char c = buffer[pos];
    String keyword;
    String keywordUpper;
    int peeking;
    if (c == 't' || c == 'T') {
      keyword = "true";
      keywordUpper = "TRUE";
      peeking = PEEKED_TRUE;
    } else if (c == 'f' || c == 'F') {
      keyword = "false";
      keywordUpper = "FALSE";
      peeking = PEEKED_FALSE;
    } else if (c == 'n' || c == 'N') {
      keyword = "null";
      keywordUpper = "NULL";
      peeking = PEEKED_NULL;
    } else {
      return PEEKED_NONE;
    }

    // Confirm that chars [1..length) match the keyword.
    int length = keyword.length();
    for (int i = 1; i < length; i++) {
      if (pos + i >= limit && !fillBuffer(i + 1)) {
        return PEEKED_NONE;
      }
      c = buffer[pos + i];
      if (c != keyword.charAt(i) && c != keywordUpper.charAt(i)) {
        return PEEKED_NONE;
      }
    }

    if ((pos + length < limit || fillBuffer(length + 1))
        && isLiteral(buffer[pos + length])) {
      return PEEKED_NONE; // Don't match trues, falsey or nullsoft!
    }

    // We've found the keyword followed either by EOF or by a non-literal character.
    pos += length;
    return peeked = peeking;
  }

  /**
   * peekNumber() 方法和上述 peekKeyword() 方法很类似
   * 用于判断下一个要处理的字符串是否是数字
   */
  private int peekNumber() throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;

    long value = 0; // Negative to accommodate Long.MIN_VALUE more easily.
    boolean negative = false;
    boolean fitsInLong = true;
    int last = NUMBER_CHAR_NONE;

    int i = 0;

    charactersOfNumber:
    for (; true; i++) {
      if (p + i == l) {
        if (i == buffer.length) {
          // Though this looks like a well-formed number, it's too long to continue reading. Give up
          // and let the application handle this as an unquoted literal.
          return PEEKED_NONE;
        }
        if (!fillBuffer(i + 1)) {
          break;
        }
        p = pos;
        l = limit;
      }

      char c = buffer[p + i];
      switch (c) {
      case '-':
        if (last == NUMBER_CHAR_NONE) {
          negative = true;
          last = NUMBER_CHAR_SIGN;
          continue;
        } else if (last == NUMBER_CHAR_EXP_E) {
          last = NUMBER_CHAR_EXP_SIGN;
          continue;
        }
        return PEEKED_NONE;

      case '+':
        if (last == NUMBER_CHAR_EXP_E) {
          last = NUMBER_CHAR_EXP_SIGN;
          continue;
        }
        return PEEKED_NONE;

      case 'e':
      case 'E':
        if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT) {
          last = NUMBER_CHAR_EXP_E;
          continue;
        }
        return PEEKED_NONE;

      case '.':
        if (last == NUMBER_CHAR_DIGIT) {
          last = NUMBER_CHAR_DECIMAL;
          continue;
        }
        return PEEKED_NONE;

      default:
        if (c < '0' || c > '9') {
          if (!isLiteral(c)) {
            break charactersOfNumber;
          }
          return PEEKED_NONE;
        }
        if (last == NUMBER_CHAR_SIGN || last == NUMBER_CHAR_NONE) {
          value = -(c - '0');
          last = NUMBER_CHAR_DIGIT;
        } else if (last == NUMBER_CHAR_DIGIT) {
          if (value == 0) {
            return PEEKED_NONE; // Leading '0' prefix is not allowed (since it could be octal).
          }
          long newValue = value * 10 - (c - '0');
          fitsInLong &= value > MIN_INCOMPLETE_INTEGER
              || (value == MIN_INCOMPLETE_INTEGER && newValue < value);
          value = newValue;
        } else if (last == NUMBER_CHAR_DECIMAL) {
          last = NUMBER_CHAR_FRACTION_DIGIT;
        } else if (last == NUMBER_CHAR_EXP_E || last == NUMBER_CHAR_EXP_SIGN) {
          last = NUMBER_CHAR_EXP_DIGIT;
        }
      }
    }

    // We've read a complete number. Decide if it's a PEEKED_LONG or a PEEKED_NUMBER.
    if (last == NUMBER_CHAR_DIGIT && fitsInLong && (value != Long.MIN_VALUE || negative) && (value!=0 || false==negative)) {
      peekedLong = negative ? value : -value;
      pos += i;
      return peeked = PEEKED_LONG;
    } else if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT
        || last == NUMBER_CHAR_EXP_DIGIT) {
      peekedNumberLength = i;
      return peeked = PEEKED_NUMBER;
    } else {
      return PEEKED_NONE;
    }
  }

  private boolean isLiteral(char c) throws IOException {
    switch (c) {
    case '/':
    case '\\':
    case ';':
    case '#':
    case '=':
      checkLenient(); // fall-through
    case '{':
    case '}':
    case '[':
    case ']':
    case ':':
    case ',':
    case ' ':
    case '\t':
    case '\f':
    case '\r':
    case '\n':
      return false;
    default:
      return true;
    }
  }

  /**
   * Returns the next token, a {@link com.google.gson.stream.JsonToken#NAME property name}, and
   * consumes it.
   *
   * <p>返回下一个token，值是 {@link com.google.gson.stream.JsonToken#NAME 属性名称}，以及consume 它
   *
   * @throws java.io.IOException if the next token in the stream is not a property
   *     name.
   */
  public String nextName() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED_NAME) {
      result = nextUnquotedValue(); // 读取一个字符串，来到这里
    } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
      result = nextQuotedValue('"');
    } else {
      throw new IllegalStateException("Expected a name but was " + peek() + locationString());
    }
    peeked = PEEKED_NONE;
    pathNames[stackSize - 1] = result;
    return result;
  }

  /**
   * Returns the {@link com.google.gson.stream.JsonToken#STRING string} value of the next token,
   * consuming it. If the next token is a number, this method will return its
   * string form.
   *
   * @throws IllegalStateException if the next token is not a string or if
   *     this reader is closed.
   */
  public String nextString() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED) {
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED) { // String 字符串对应的是8
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED) {
      result = nextQuotedValue('"');
    } else if (p == PEEKED_BUFFERED) {
      result = peekedString;
      peekedString = null;
    } else if (p == PEEKED_LONG) {
      result = Long.toString(peekedLong);
    } else if (p == PEEKED_NUMBER) {
      result = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else {
      throw new IllegalStateException("Expected a string but was " + peek() + locationString());
    }
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the {@link com.google.gson.stream.JsonToken#BOOLEAN boolean} value of the next token,
   * consuming it.
   *
   * @throws IllegalStateException if the next token is not a boolean or if
   *     this reader is closed.
   */
  public boolean nextBoolean() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_TRUE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return true;
    } else if (p == PEEKED_FALSE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return false;
    }
    throw new IllegalStateException("Expected a boolean but was " + peek() + locationString());
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is a
   * literal null.
   *
   * @throws IllegalStateException if the next token is not null or if this
   *     reader is closed.
   */
  public void nextNull() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_NULL) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
    } else {
      throw new IllegalStateException("Expected null but was " + peek() + locationString());
    }
  }

  /**
   * Returns the {@link com.google.gson.stream.JsonToken#NUMBER double} value of the next token,
   * consuming it. If the next token is a string, this method will attempt to
   * parse it as a double using {@link Double#parseDouble(String)}.
   *
   * @throws IllegalStateException if the next token is not a literal value.
   * @throws NumberFormatException if the next literal value cannot be parsed
   *     as a double, or is non-finite.
   */
  public double nextDouble() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return (double) peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED) {
      peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
    } else if (p == PEEKED_UNQUOTED) {
      peekedString = nextUnquotedValue();
    } else if (p != PEEKED_BUFFERED) {
      throw new IllegalStateException("Expected a double but was " + peek() + locationString());
    }

    peeked = PEEKED_BUFFERED;
    double result = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    if (!lenient && (Double.isNaN(result) || Double.isInfinite(result))) {
      throw new MalformedJsonException(
          "JSON forbids NaN and infinities: " + result + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the {@link com.google.gson.stream.JsonToken#NUMBER long} value of the next token,
   * consuming it. If the next token is a string, this method will attempt to
   * parse it as a long. If the next token's numeric value cannot be exactly
   * represented by a Java {@code long}, this method throws.
   *
   * @throws IllegalStateException if the next token is not a literal value.
   * @throws NumberFormatException if the next literal value cannot be parsed
   *     as a number, or exactly represented as a long.
   */
  public long nextLong() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        long result = Long.parseLong(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    } else {
      throw new IllegalStateException("Expected a long but was " + peek() + locationString());
    }

    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    long result = (long) asDouble;
    if (result != asDouble) { // Make sure no precision was lost casting to 'long'.
      throw new NumberFormatException("Expected a long but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the string up to but not including {@code quote}, unescaping any
   * character escape sequences encountered along the way. The opening quote
   * should have already been read. This consumes the closing quote, but does
   * not include it in the returned string.
   *
   * <p>直到遇到 {@code quote} 的字符串就返回。对中间遇到的任何字符转义序列进行转义。
   * <p>这个过程最后一步消耗一个结束的引号，但是不会将其包含在返回的字符串中。
   *
   * @param quote either ' or ".
   * @throws NumberFormatException if any unicode escape sequences are
   *     malformed.
   */
  private String nextQuotedValue(char quote) throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    StringBuilder builder = null;
    while (true) {
      int p = pos; // pos = 7，是在value字符串的左边的'
      int l = limit;
      /* the index of the first character not yet appended to the builder. */
      int start = p;
      while (p < l) {
        int c = buffer[p++]; // 在这里让指针p向右移动

        if (c == quote) {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            return new String(buffer, start, len); // start 7， 长度 5
          } else {
            builder.append(buffer, start, len);
            return builder.toString();
          }
        } else if (c == '\\') {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            int estimatedLength = (len + 1) * 2;
            builder = new StringBuilder(Math.max(estimatedLength, 16));
          }
          builder.append(buffer, start, len);
          builder.append(readEscapeCharacter());
          p = pos;
          l = limit;
          start = p;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }

      if (builder == null) {
        int estimatedLength = (p - start) * 2;
        builder = new StringBuilder(Math.max(estimatedLength, 16));
      }
      builder.append(buffer, start, p - start);
      pos = p;
      if (!fillBuffer(1)) {
        throw syntaxError("Unterminated string");
      }
    }
  }

  /**
   * Returns an unquoted value as a string.
   */
  @SuppressWarnings("fallthrough")
  private String nextUnquotedValue() throws IOException {
    StringBuilder builder = null;
    int i = 0;

    findNonLiteralCharacter:
    while (true) { // 在这个循环里面，当下一个是罗列的这些字符的话，就退出，说明读取到一个完整的字符串了
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
        case '/':
        case '\\':
        case ';':
        case '#':
        case '=':
          checkLenient(); // fall-through
        case '{':
        case '}':
        case '[':
        case ']':
        case ':':
        case ',':
        case ' ':
        case '\t':
        case '\f':
        case '\r':
        case '\n':
          break findNonLiteralCharacter; // 当是空格的时候跳出
        }
      }

      // Attempt to load the entire literal into the buffer at once.
      if (i < buffer.length) {
        if (fillBuffer(i + 1)) {
          continue;
        } else {
          break;
        }
      }

      // use a StringBuilder when the value is too long. This is too long to be a number!
      if (builder == null) {
        builder = new StringBuilder(Math.max(i,16));
      }
      builder.append(buffer, pos, i);
      pos += i;
      i = 0;
      if (!fillBuffer(1)) {
        break;
      }
    }
   
    String result = (null == builder) ? new String(buffer, pos, i) : builder.append(buffer, pos, i).toString();
    pos += i;
    return result;
  }

  private void skipQuotedValue(char quote) throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    do {
      int p = pos;
      int l = limit;
      /* the index of the first character not yet appended to the builder. */
      while (p < l) {
        int c = buffer[p++];
        if (c == quote) {
          pos = p;
          return;
        } else if (c == '\\') {
          pos = p;
          readEscapeCharacter();
          p = pos;
          l = limit;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }
      pos = p;
    } while (fillBuffer(1));
    throw syntaxError("Unterminated string");
  }

  private void skipUnquotedValue() throws IOException {
    do {
      int i = 0;
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
        case '/':
        case '\\':
        case ';':
        case '#':
        case '=':
          checkLenient(); // fall-through
        case '{':
        case '}':
        case '[':
        case ']':
        case ':':
        case ',':
        case ' ':
        case '\t':
        case '\f':
        case '\r':
        case '\n':
          pos += i;
          return;
        }
      }
      pos += i;
    } while (fillBuffer(1));
  }

  /**
   * Returns the {@link com.google.gson.stream.JsonToken#NUMBER int} value of the next token,
   * consuming it. If the next token is a string, this method will attempt to
   * parse it as an int. If the next token's numeric value cannot be exactly
   * represented by a Java {@code int}, this method throws.
   *
   * @throws IllegalStateException if the next token is not a literal value.
   * @throws NumberFormatException if the next literal value cannot be parsed
   *     as a number, or exactly represented as an int.
   */
  public int nextInt() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    int result;
    if (p == PEEKED_LONG) {
      result = (int) peekedLong;
      if (peekedLong != result) { // Make sure no precision was lost casting to 'int'.
        throw new NumberFormatException("Expected an int but was " + peekedLong + locationString());
      }
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return result;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        result = Integer.parseInt(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    } else {
      throw new IllegalStateException("Expected an int but was " + peek() + locationString());
    }

    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    result = (int) asDouble;
    if (result != asDouble) { // Make sure no precision was lost casting to 'int'.
      throw new NumberFormatException("Expected an int but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Closes this JSON reader and the underlying {@link java.io.Reader}.
   */
  public void close() throws IOException {
    peeked = PEEKED_NONE;
    stack[0] = JsonScope.CLOSED;
    stackSize = 1;
    in.close();
  }

  /**
   * Skips the next value recursively. If it is an object or array, all nested
   * elements are skipped. This method is intended for use when the JSON token
   * stream contains unrecognized or unhandled values.
   */
  public void skipValue() throws IOException {
    int count = 0;
    do {
      int p = peeked;
      if (p == PEEKED_NONE) {
        p = doPeek();
      }

      if (p == PEEKED_BEGIN_ARRAY) {
        push(JsonScope.EMPTY_ARRAY);
        count++;
      } else if (p == PEEKED_BEGIN_OBJECT) {
        push(JsonScope.EMPTY_OBJECT);
        count++;
      } else if (p == PEEKED_END_ARRAY) {
        stackSize--;
        count--;
      } else if (p == PEEKED_END_OBJECT) {
        stackSize--;
        count--;
      } else if (p == PEEKED_UNQUOTED_NAME || p == PEEKED_UNQUOTED) {
        skipUnquotedValue();
      } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_SINGLE_QUOTED_NAME) {
        skipQuotedValue('\'');
      } else if (p == PEEKED_DOUBLE_QUOTED || p == PEEKED_DOUBLE_QUOTED_NAME) {
        skipQuotedValue('"');
      } else if (p == PEEKED_NUMBER) {
        pos += peekedNumberLength;
      }
      peeked = PEEKED_NONE;
    } while (count != 0);

    pathIndices[stackSize - 1]++;
    pathNames[stackSize - 1] = "null";
  }

  private void push(int newTop) {
    if (stackSize == stack.length) { // 能进来的时候应该是到最后的时候
      int newLength = stackSize * 2;
      stack = Arrays.copyOf(stack, newLength);
      pathIndices = Arrays.copyOf(pathIndices, newLength);
      pathNames = Arrays.copyOf(pathNames, newLength);
    }
    stack[stackSize++] = newTop;
  }

  /**
   * Returns true once {@code limit - pos >= minimum}. If the data is
   * exhausted before that many characters are available, this returns
   * false.
   *
   * 如果还有字符，但是buffer数组已经满了，那么返回false
   */
  private boolean fillBuffer(int minimum) throws IOException {
    char[] buffer = this.buffer;
    lineStart -= pos;
    if (limit != pos) {
      limit -= pos;
      System.arraycopy(buffer, pos, buffer, 0, limit);
    } else {
      limit = 0;
    }

    pos = 0;
    int total;
    // 将字节流读取到buffer数组中
    while ((total = in.read(buffer, limit, buffer.length - limit)) != -1) {
      limit += total;

      // if this is the first read, consume an optional byte order mark (BOM) if it exists
      if (lineNumber == 0 && lineStart == 0 && limit > 0 && buffer[0] == '\ufeff') {
        pos++;
        lineStart++;
        minimum++;
      }

      if (limit >= minimum) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the next character in the stream that is neither whitespace nor a
   * part of a comment. When this returns, the returned character is always at
   * {@code buffer[pos-1]}; this means the caller can always push back the
   * returned character by decrementing {@code pos}.
   *
   * <p>
   *     返回流中的下一个既不是空格也不是注释的一部分的字符。
   *     当它返回时，返回的字符总是在 {@code buffer[pos-1]}; 这意味着调用者总是可以通过递减 {@code pos} 来推回返回的字符。
   * </p>
   */
  private int nextNonWhitespace(boolean throwOnEof) throws IOException {
    /*
     * This code uses ugly local variables 'p' and 'l' representing the 'pos'
     * and 'limit' fields respectively. Using locals rather than fields saves
     * a few field reads for each whitespace character in a pretty-printed
     * document, resulting in a 5% speedup. We need to flush 'p' to its field
     * before any (potentially indirect) call to fillBuffer() and reread both
     * 'p' and 'l' after any (potentially indirect) call to the same method.
     */
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;
    while (true) {
      // 第一次读取的时候会走到if里面
      if (p == l) {
        pos = p;
        // 填充字符数组
        if (!fillBuffer(1)) {
          break;
        }
        p = pos;
        l = limit;
      }

      // 之后会来到这里，读取之后++
      int c = buffer[p++];
      if (c == '\n') {
        lineNumber++;
        lineStart = p;
        continue;
      } else if (c == ' ' || c == '\r' || c == '\t') {
        continue;
      }

      if (c == '/') {
        pos = p;
        if (p == l) {
          pos--; // push back '/' so it's still in the buffer when this method returns
          boolean charsLoaded = fillBuffer(2);
          pos++; // consume the '/' again
          if (!charsLoaded) {
            return c;
          }
        }

        checkLenient();
        char peek = buffer[pos];
        switch (peek) {
        case '*':
          // skip a /* c-style comment */
          pos++;
          if (!skipTo("*/")) {
            throw syntaxError("Unterminated comment");
          }
          p = pos + 2;
          l = limit;
          continue;

        case '/':
          // skip a // end-of-line comment
          pos++;
          skipToEndOfLine();
          p = pos;
          l = limit;
          continue;

        default:
          return c;
        }
      } else if (c == '#') {
        pos = p;
        /*
         * Skip a # hash end-of-line comment. The JSON RFC doesn't
         * specify this behaviour, but it's required to parse
         * existing documents. See http://b/2571423.
         */
        checkLenient();
        skipToEndOfLine();
        p = pos;
        l = limit;
      } else {
        pos = p;
        // 返回char对应的int值
        return c;
      }
    }
    if (throwOnEof) {
      throw new EOFException("End of input" + locationString());
    } else {
      return -1;
    }
  }

  private void checkLenient() throws IOException {
    if (!lenient) {
      throw syntaxError("Use JsonReader.setLenient(true) to accept malformed JSON");
    }
  }

  /**
   * Advances the position until after the next newline character. If the line
   * is terminated by "\r\n", the '\n' must be consumed as whitespace by the
   * caller.
   */
  private void skipToEndOfLine() throws IOException {
    while (pos < limit || fillBuffer(1)) {
      char c = buffer[pos++];
      if (c == '\n') {
        lineNumber++;
        lineStart = pos;
        break;
      } else if (c == '\r') {
        break;
      }
    }
  }

  /**
   * @param toFind a string to search for. Must not contain a newline.
   */
  private boolean skipTo(String toFind) throws IOException {
    int length = toFind.length();
    outer:
    for (; pos + length <= limit || fillBuffer(length); pos++) {
      if (buffer[pos] == '\n') {
        lineNumber++;
        lineStart = pos + 1;
        continue;
      }
      for (int c = 0; c < length; c++) {
        if (buffer[pos + c] != toFind.charAt(c)) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  @Override public String toString() {
    return getClass().getSimpleName() + locationString();
  }

  String locationString() {
    int line = lineNumber + 1;
    int column = pos - lineStart + 1;
    return " at line " + line + " column " + column + " path " + getPath();
  }

  /**
   * Returns a <a href="http://goessner.net/articles/JsonPath/">JsonPath</a> to
   * the current location in the JSON value.
   */
  public String getPath() {
    StringBuilder result = new StringBuilder().append('$');
    for (int i = 0, size = stackSize; i < size; i++) {
      switch (stack[i]) {
        case JsonScope.EMPTY_ARRAY:
        case JsonScope.NONEMPTY_ARRAY:
          result.append('[').append(pathIndices[i]).append(']');
          break;

        case JsonScope.EMPTY_OBJECT:
        case JsonScope.DANGLING_NAME:
        case JsonScope.NONEMPTY_OBJECT:
          result.append('.');
          if (pathNames[i] != null) {
            result.append(pathNames[i]);
          }
          break;

        case JsonScope.NONEMPTY_DOCUMENT:
        case JsonScope.EMPTY_DOCUMENT:
        case JsonScope.CLOSED:
          break;
      }
    }
    return result.toString();
  }

  /**
   * Unescapes the character identified by the character or characters that
   * immediately follow a backslash. The backslash '\' should have already
   * been read. This supports both unicode escapes "u000A" and two-character
   * escapes "\n".
   *
   * @throws NumberFormatException if any unicode escape sequences are
   *     malformed.
   */
  private char readEscapeCharacter() throws IOException {
    if (pos == limit && !fillBuffer(1)) {
      throw syntaxError("Unterminated escape sequence");
    }

    char escaped = buffer[pos++];
    switch (escaped) {
    case 'u':
      if (pos + 4 > limit && !fillBuffer(4)) {
        throw syntaxError("Unterminated escape sequence");
      }
      // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
      char result = 0;
      for (int i = pos, end = i + 4; i < end; i++) {
        char c = buffer[i];
        result <<= 4;
        if (c >= '0' && c <= '9') {
          result += (c - '0');
        } else if (c >= 'a' && c <= 'f') {
          result += (c - 'a' + 10);
        } else if (c >= 'A' && c <= 'F') {
          result += (c - 'A' + 10);
        } else {
          throw new NumberFormatException("\\u" + new String(buffer, pos, 4));
        }
      }
      pos += 4;
      return result;

    case 't':
      return '\t';

    case 'b':
      return '\b';

    case 'n':
      return '\n';

    case 'r':
      return '\r';

    case 'f':
      return '\f';

    case '\n':
      lineNumber++;
      lineStart = pos;
      // fall-through

    case '\'':
    case '"':
    case '\\':
    case '/':	
    	return escaped;
    default:
    	// throw error when none of the above cases are matched
    	throw syntaxError("Invalid escape sequence");
    }
  }

  /**
   * Throws a new IO exception with the given message and a context snippet
   * with this reader's content.
   */
  private IOException syntaxError(String message) throws IOException {
    throw new MalformedJsonException(message + locationString());
  }

  /**
   * Consumes the non-execute prefix if it exists.
   */
  private void consumeNonExecutePrefix() throws IOException {
    // fast forward through the leading whitespace 向前推移写空间
    // 在第一次调用 nextNonWhitespace(true) 方法的时候，json 字符串会被转存为一个 char 数组
    nextNonWhitespace(true);
    // 在nextNonWhitespace这里往前+1，这里又-1，变为0
    pos--;

    int p = pos;
    if (p + 5 > limit && !fillBuffer(5)) {
      return;
    }

    char[] buf = buffer;
    if(buf[p] != ')' || buf[p + 1] != ']' || buf[p + 2] != '}' || buf[p + 3] != '\'' || buf[p + 4] != '\n') {
      return; // not a security token!
    }

    // we consumed a security token!
    pos += 5;
  }

  // 这是一段静态代码块，创建对象的时候会执行到这里来
  static {
    JsonReaderInternalAccess.INSTANCE = new JsonReaderInternalAccess() { // JsonReaderInternalAccess是一个抽象类，里面有一个成员变量
      @Override public void promoteNameToValue(JsonReader reader) throws IOException {
        if (reader instanceof JsonTreeReader) {
          ((JsonTreeReader)reader).promoteNameToValue();
          return;
        }
        int p = reader.peeked;
        if (p == PEEKED_NONE) {
          p = reader.doPeek();
        }
        if (p == PEEKED_DOUBLE_QUOTED_NAME) {
          reader.peeked = PEEKED_DOUBLE_QUOTED;
        } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
          reader.peeked = PEEKED_SINGLE_QUOTED;
        } else if (p == PEEKED_UNQUOTED_NAME) {
          reader.peeked = PEEKED_UNQUOTED;
        } else {
          throw new IllegalStateException(
              "Expected a name but was " + reader.peek() + reader.locationString());
        }
      }
    };
  }
}
