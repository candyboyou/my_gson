package com.google.gson.mytest;

import org.junit.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleDateFormatTest{

    static ThreadLocal<SimpleDateFormat> safeDateFormat = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(100, 100, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000));

    @Test
    public void test() {
        while (true) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String dateString = safeDateFormat.get().format(new Date());
                    try {
                        Date parseDate = safeDateFormat.get().parse(dateString);
                        String dateString2 = safeDateFormat.get().format(parseDate);
                        System.out.println(dateString.equals(dateString2));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Test
    public void test2() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setAddressEmail("hhhh" + i + "@qq.com"); ;
            user.setId(i + 1000);
            user.setName("hhhh" + i);
            users.add(user);
        }

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    User user = users.get(finalI);
                    System.out.println();
                }
            });
        }
    }

    @Test
    public void test3() {
        ArrayList<Integer> arrayList = new ArrayList<>(10);
        try {
            Field dateField = arrayList.getClass().getDeclaredField("elementData");
            dateField.setAccessible(true);
            int length = ((Object[]) dateField.get(arrayList)).length;
            System.out.println(length);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
