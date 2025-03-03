package com.example;
import com.google.gson.*;
import java.lang.reflect.Type;

public class main {
    public static void main(String[] args) {
        String goodJson = "{\"name\":\"ivan\"}";
        String badJson = "{\"nickname\":\"ivan\"}";
        Gson goodGson = new GsonBuilder().registerTypeAdapter(User.class, new UserGoodDeserializer()).create();
        Gson badGson  = new GsonBuilder().registerTypeAdapter(User.class, new UserBadDeserializer()).create();

        // System.out.println("===== Deserializing valid JSON: " + goodJson);
        // User user1 = goodGson.fromJson(goodJson, User.class);
        // System.out.println("Good deserializer: " + user1);

        // User user2 = goodGson.fromJson(goodJson, User.class);
        // System.out.println("Bad deserializer:  " + user2);
        // System.out.println("===== Deserializing invalid JSON: " + badJson);
        // try {
        //     goodGson.fromJson(badJson, User.class);
        // } catch (JsonSyntaxException e) {
        //     System.out.println("Good deserializer caught JsonSyntaxException");
        // }

        try {
            try {
                badGson.fromJson(badJson, User.class);
            } catch (JsonSyntaxException e) {
                System.out.println("Bad deserializer caught JsonSyntaxException");
            }
        } catch (RuntimeException e) {
            // Print out the full stack trace to see the exception type
            e.printStackTrace();
            System.out.println("Bad deserializer did not catch JsonSyntaxException, and " + e.getClass().getSimpleName() + " propagated further.");
        }

    }
}

class User {
    String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "'}";
    }
}

class UserGoodDeserializer implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        if (!jsonObject.has("name")) throw new JsonSyntaxException("1json should contain the name field!");
        String name = jsonObject.get("name").getAsString();
        return new User(name);
    }
}

class UserBadDeserializer implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        if (!jsonObject.has("name")) throw new JsonParseException("2json should contain the name field!");
        String name = jsonObject.get("name").getAsString();
        return new User(name);
    }
}
