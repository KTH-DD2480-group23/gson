package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
//import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
//import com.google.gson.reflect.TypeToken;
//import com.google.gson.stream.JsonReader;
// import com.google.gson.stream.JsonWriter;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

// import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

//import java.io.IOException;
//import java.io.StringReader;
// import java.io.StringWriter;
// import java.util.LinkedHashMap;
// import java.util.Map;

public class FlatteningTypeAdapterFactoryTest {
    
    class FlatModel1 {
        @SerializedName("a.b")
        public int aB;
        
        @SerializedName("a.c") 
        public boolean aC;
    }

    @Test
    public void testFlatteningtoModelClass() {

        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\": {\"b\": 1, \"c\": true}}";
        FlatModel1 result = gson.fromJson(json, FlatModel1.class);
        
        // Check the flattened fields
        assertEquals(1, result.aB);
        assertTrue(result.aC);
    }
    
    @Test
    public void testDottedKeyJsontoModel() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a.b\": 1, \"a.c\": true}";
        assertThrows(IllegalArgumentException.class, () -> {
            gson.fromJson(json, FlatModel1.class);
        });
    }

    @Test
    public void testConflictMixedWithModelClass() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\":{\"b\":-1}, \"a.b\": 1, \"a.c\": true}";
        assertThrows(IllegalArgumentException.class, () -> {
            gson.fromJson(json, FlatModel1.class);
        });
    }
    class FlatModel2 {
        @SerializedName("a.1.@")
        public double a1;
        
        @SerializedName("a.2") 
        public String a2;
    }

    @Test
    public void testFlatteningWithSpecialCharacters() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\": {\"1\": {\"@\": 1.01}, \"2\": \"a.1.@\"}}";
        FlatModel2 result = gson.fromJson(json, FlatModel2.class);
        
        // Check the flattened fields
        assertEquals(1.01, result.a1, 0.0);
        assertEquals("a.1.@", result.a2);
    }

    @Test
    public void testUnflattenfromModelClass() {
        FlatModel1 model = new FlatModel1();
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        model.aB = 2147483647;
        model.aC = true;
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        JsonObject json = gson.toJsonTree(model).getAsJsonObject();
        assertTrue(json.has("a"));
        JsonObject a = json.getAsJsonObject("a");
        assertTrue(a.has("b"));
        assertEquals(2147483647, a.get("b").getAsInt());
        assertTrue(a.has("c"));
        assertTrue(a.get("c").getAsBoolean());
    }
}