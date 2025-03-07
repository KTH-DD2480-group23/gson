package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import com.google.gson.JsonObject;
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
    
    class FlatModel {
        @SerializedName("a.b")
        public int aB;
        
        @SerializedName("a.c") 
        public boolean aC;
    }

    @Test
    public void testFlatteningWithModelClass() {

        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\": {\"b\": 1, \"c\": true}}";
        FlatModel result = gson.fromJson(json, FlatModel.class);
        
        // Check the flattened fields
        assertEquals(1, result.aB);
        assertTrue(result.aC);
    }
    
    @Test
    public void testAlreadyFlattenedWithModelClass() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a.b\": 1, \"a.c\": true}";
        FlatModel result = gson.fromJson(json, FlatModel.class);
        
        // Check the flattened fields
        assertEquals(1, result.aB);
        assertTrue(result.aC);
    }
    @Test
    public void testnonConflictMixWithModelClass() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\":{\"b\":-1}, \"a.c\": true}";
        FlatModel result = gson.fromJson(json, FlatModel.class);
        
        // Check the flattened fields
        assertEquals(-1, result.aB);
        assertTrue(result.aC);
    }

    @Test
    public void testConflictMixWithModelClass() {
        FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(factory)
            .create();
        
        String json = "{\"a\":{\"b\":-1}, \"a.b\": 1, \"a.c\": true}";
        FlatModel result = gson.fromJson(json, FlatModel.class);
        System.out.println( "Findme"+ result.aB);
        assertThrows(IllegalArgumentException.class, () -> {
            gson.fromJson(json, FlatModel.class);
        });
    }
}