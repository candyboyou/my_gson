package com.google.gson;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateDeserializer implements JsonDeserializer<Date>{

    private SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.US);

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context){
        String j = json.getAsJsonPrimitive().getAsString();
        return parseDate(j);
    }

    private Date parseDate(String dateString){
        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
