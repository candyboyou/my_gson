package com.google.gson.mytest;

import com.google.gson.annotations.Since;
import lombok.AllArgsConstructor;
import lombok.Data;

//@Data
public class User {

    private Integer id;

    private String name;

    @Since(1.3)
    private String addressEmail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressEmail() {
        return addressEmail;
    }

    public void setAddressEmail(String addressEmail) {
        this.addressEmail = addressEmail;
    }
}
