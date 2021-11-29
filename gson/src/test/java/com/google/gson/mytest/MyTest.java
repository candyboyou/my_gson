package com.google.gson.mytest;

import com.google.gson.*;
import com.google.gson.annotations.*;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

public class MyTest extends TestCase {

    /**
     1. 没有被@Expose注解的password将不会参与序列化及反序列化。
     2. lastName 不会参与序列化
     3. emailAddress 不会参与序列化及反序列化
    */
    public void testExpose() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        UserWithExpose user = new UserWithExpose("firstName", "lastName", "123@qq.com");
        System.out.println(gson.toJson(user)); // {"firstName":"firstName"}
    }

    /*
    我们自定义的JsonAdapter需要配合 GsonBuilder.registerTypeAdapter() 使用，
    但每次使用都要注册也太麻烦了，JsonAdapter注解就是为了解决这个痛点的。
    使用时就不需要再使用 GsonBuilder去注册 UserTypeAdapter 了
     */
    @JsonAdapter(UserTypeAdapter.class)
    class UserWithExpose {
        @Expose private String firstName;
        @Expose(serialize = false) private String lastName;
        @Expose (serialize = false, deserialize = false) private String emailAddress;

        UserWithExpose(String firstName, String lastName, String emailAddress) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.emailAddress = emailAddress;
        }
    }

    class ResponseBean{
        @SerializedName("public")
        private String pub;
        @SerializedName("class")
        private String cla;
        @SerializedName("static")
        private String sta;

        @Override
        public java.lang.String toString() {
            return "ResponseBean{" +
                    "pub='" + pub + '\'' +
                    ", cla='" + cla + '\'' +
                    ", sta='" + sta + '\'' +
                    '}';
        }
    }

    public void testSerializeName() {
        String json = "{\n" +
                "    \"public\":\"1\",\n" +
                "    \"class\":\"2\",\n" +
                "    \"static\":\"3\"\n" +
                "}";
        Gson gson = new Gson();
        ResponseBean responseBean = gson.fromJson(json, ResponseBean.class);
        System.out.println(responseBean); // ResponseBean{pub='1', cla='2', sta='3'}
    }

    @Data
    @AllArgsConstructor
    class SinceUntilSample {
        @Since(1.1) public String sinceField; //大于等于Since
        @Until(1.5) public String untilField; //小于Until
        @Since(1.2) @Until(2.0) public String allField; //大于等于Since且小于Until
    }

    public void testSince() {
//        SinceUntilSample sample = new SinceUntilSample("since", "util", "all");
//        Gson gson = new GsonBuilder().setVersion(1.1).create();
//        Gson gson = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_DOTS).create();
//        System.out.println(gson.toJson(sample)); // util all
    }

    /**
    按照修饰符过滤
     */
    class ModifierSample {
        final String finalField = "final";
        static String staticField = "static";
        public String publicField = "public";
        protected String protectedField = "protected";
        String defaultField = "default";
        private String privateField = "private";
    }

    public void testModifier() {
        ModifierSample modifierSample = new ModifierSample();
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PRIVATE)
                .create();
        System.out.println(gson.toJson(modifierSample));
        // {"publicField":"public","protectedField":"protected","defaultField":"default"}
    }

    class TestExclusionStrategyDemo {
        String finalField = "final";
        String staticField = "static";
        String excludeField = "excludeField";
        int num = 1;
    }

    /**
     * 基于策略（自定义规则）
     */
    public void testExclusionStrategy() {
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        // 这里作判断，决定要不要排除该字段,return true为排除
                        if ("excludeField".equals(f.getName())) return true; //按字段名排除
                        Expose expose = f.getAnnotation(Expose.class);
                        if (expose != null && expose.deserialize() == false) return true; //按注解排除
                        return false;
                    }
                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        // 直接排除某个类 ，return true为排除
                        return (clazz == int.class || clazz == Integer.class);
                    }
                })
                .create();
        String s = gson.toJson(new TestExclusionStrategyDemo()); //{"finalField":"final","staticField":"static"}
    }

    public void testTypeToken() {
        Gson gson = new Gson();
        String jsonArray = "[\"Android\",\"Java\",\"PHP\"]";
//        List list = gson.fromJson(jsonArray, List<String>.class);
        String[] strings = gson.fromJson(jsonArray, String[].class); // String[].class 直接改为 List<String>.class 是行不通的。
        List<String> list = gson.fromJson(jsonArray, new TypeToken<List<String>>(){}.getType());

        // 对于data字段是User时则可以写为 Result<User> ,当是个列表的时候为 Result<List<User>>
        String json = "{\"code\":\"0\",\"message\":\"success\",\"data\":{}}";
        Type userType = new TypeToken<Result<User>>(){}.getType();
        Result<User> userResult = gson.fromJson(json,userType);
        User user = userResult.data;

        Type userListType = new TypeToken<Result<List<User>>>(){}.getType();
        Result<List<User>> userListResult = gson.fromJson(json,userListType);
        List<User> users = userListResult.data;
    }

    class Result<T> {
        public int code;
        public String message;
        public T data;
    }

    /**
     * | FieldNamingPolicy            | 结果（仅输出emailAddress字段）  |
     * | ---------------------------- | ------------------------------- |
     * | IDENTITY                     | {"emailAddress":"123@126.com"}  |
     * | LOWER_CASE_WITH_DASHES       | {"email-address":"123@126.com"} |
     * | LOWER_CASE_WITH_UNDERSCORES  | {"email_address":"123@126.com"} |
     * | UPPER_CAMEL_CASE             | {"EmailAddress":"123@126.com"}  |
     * | UPPER_CAMEL_CASE_WITH_SPACES | {"Email Address":"123@126.com"} |
     */
    public void test2() {
        String json = "{name:'user1', id:1, email:'1234@qq.com'}";
        Gson gson = new Gson();
        User user = gson.fromJson(json, User.class);
    }

}
