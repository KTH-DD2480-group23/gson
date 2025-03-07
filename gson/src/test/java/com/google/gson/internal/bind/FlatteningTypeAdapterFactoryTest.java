package com.google.gson.internal.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.junit.Test;

public class FlatteningTypeAdapterFactoryTest {

  static class FlatModel1 {
    @SerializedName("a.b")
    public int aB;

    @SerializedName("a.c")
    public boolean aC;
  }

  @Test
  public void testFlatteningtoModelClass() { 

    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a\": {\"b\": 1, \"c\": true}}";
    FlatModel1 result = gson.fromJson(json, FlatModel1.class); //Requirement: 1 

    // Check the flattened fields
    assertEquals(1, result.aB); //Requirement: 2
    assertTrue(result.aC);
  }

  @Test
  public void testDottedKeyJsontoModel() {
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a.b\": 1, \"a.c\": true}";
    assertThrows( // Requirement: 4
        IllegalArgumentException.class,
        () -> {
          gson.fromJson(json, FlatModel1.class);
        });
  }

  @Test
  public void testConflictMixedWithModelClass() {
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a\":{\"b\":-1}, \"a.b\": 1, \"a.c\": true}"; 
    assertThrows( //requirement: 4
        IllegalArgumentException.class,
        () -> {
          gson.fromJson(json, FlatModel1.class);
        });
  }

  static class FlatModel2 {
    @SerializedName("a.1.@")
    public double a1;

    @SerializedName("a.2")
    public String a2;
  }

  @Test
  public void testFlatteningWithSpecialCharacters() {
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a\": {\"1\": {\"@\": 1.01}, \"2\": \"a.1.@\"}}";
    FlatModel2 result = gson.fromJson(json, FlatModel2.class);//Requirement: 1 

    // Check the flattened fields
    assertEquals(1.01, result.a1, 0.0);//Requirement: 2
    assertEquals("a.1.@", result.a2);
  }

  @Test
  public void testUnflattenfromModelClass() {
    FlatModel1 model = new FlatModel1();
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    model.aB = 2147483647;
    model.aC = true;
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();//Requirement: 1
    JsonObject json = gson.toJsonTree(model).getAsJsonObject();//Requirement: 3
    assertTrue(json.has("a"));
    JsonObject a = json.getAsJsonObject("a");
    assertTrue(a.has("b"));
    assertEquals(2147483647, a.get("b").getAsInt());
    assertTrue(a.has("c"));
    assertTrue(a.get("c").getAsBoolean());
  }

  static class FlatModel3 {
    @SerializedName("a")
    public int a;

    @SerializedName("b")
    public boolean b;

    @SerializedName("c.d")
    public int cD;

    @SerializedName("c.e")
    public String cE;
  }

  @Test
  public void testFlatteningTwoNestedWithModelClass() {
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a\": 1, \"b\": \"true\", \"c\": {\"d\": \"5\", \"e\": \"test\"}}";

    FlatModel3 result = gson.fromJson(json, FlatModel3.class);//Requirement: 1
    // Check the flattened fields
    assertEquals(1, result.a);//Requirement: 2
    assertTrue(result.b);
    assertEquals(5, result.cD);
    assertEquals("test", result.cE);
  }

  static class FlatModel4 {

    @SerializedName("a")
    public int a;

    @SerializedName("b")
    public boolean b;

    @SerializedName("c.d.f")
    public int cDf;

    @SerializedName("c.d.e")
    public String cDe;
  }

  @Test
  public void testFlatteningThreeNestedWithModelClass() {
    FlatteningTypeAdapterFactory factory = new FlatteningTypeAdapterFactory();
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(factory).create();

    String json = "{\"a\": 1, \"b\": \"true\", \"c\": {\"d\": {\"f\": 5, \"e\": \"test\"}}}";

    FlatModel4 result = gson.fromJson(json, FlatModel4.class);//Requirement: 1
    // Check the flattened fields
    assertEquals(1, result.a);//Requirement: 2
    assertTrue(result.b);
    assertEquals(5, result.cDf);
    assertEquals("test", result.cDe);
  }
}
