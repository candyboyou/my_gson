package com.google.gson.mytest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

public class MyTest extends TestCase {

    public void testSince() {
        User user = new User();
        user.setId(12);
        user.setName("candyboy");
        user.setAddressEmail("123@126.com");
//        User user = new User(1, "Gupta", "howtogoinjava@gmail.com");
//        Gson gson = new GsonBuilder().setVersion(1.1).create();
        Gson gson = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_DOTS).create();
        System.out.println(gson.toJson(user));
    }

    public void test() {
        Gson gson1 = new GsonBuilder().create();
        Gson gson = new Gson();
        String jsonArray = "[\"Android\",\"Java\",\"PHP\"]";
//        List list = gson.fromJson(jsonArray, List<String>.class);
        String[] strings = gson.fromJson(jsonArray, String[].class);
        List<String> list = gson.fromJson(jsonArray, new TypeToken<List<String>>(){}.getType());
    }

    public void test2() {
        String json = "{name:'user1', id:1, email:'1234@qq.com'}";
        Gson gson = new Gson();
        User user = gson.fromJson(json, User.class);
    }

}
