package com.google.gson.mytest;

public class DoSomeThing {

    public void doSomeThing(User user) {
        String name = user.getName();
        Integer id = user.getId();
        String email = user.getAddressEmail();
        System.out.println(name + "--" + id + "--" + email);
    }
}
