package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;

/**
 * The purpose of this file is to implement a solution to the issue:
 * https://github.com/google/gson/issues/2555, raised by the user "mpsingh47". Mpsingh47 Wanted to
 * be able to deserialize a JSON object into a flat model class without using a nested class
 * structure. Mpsingh47 requested that there should be a feature making it possible to write
 * "@serialized(address.street)", allowing the nested street value from the address to be directly
 * mapped to a field in the model. This would make it such that one does not have to create a nested
 * class to parse JSON into an object.
 *
 * <p>Under this issue, user "Marcono1234" responded with a proof of concept; however, he did not
 * implement such a feature into the gson package. This file is therefore an implementation of user
 * Marcono1234 proof of concept, accompanied by a new corresponding test file. Testing that this
 * file works correctly.
 */

/**
 * Type adapter that flattens nested JSON objects during deserialization, and expends them back to
 * their orginal format during serialization.
 */
public class FlatteningTypeAdapterFactory implements TypeAdapterFactory {
  // Called by Gson
  public FlatteningTypeAdapterFactory() {}

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    //  We first retrive the raw type of param "type" if this is a JsonPrimitive type
    //  meaning we are not dealing with a JsonObject but instead a JsonPrimitive like a
    //  int, string, Integer or any other, we will return null, meaning we will send
    //  the problem to a lower down default gson TypeAdapter create method.
    Class<? super T> raw = type.getRawType();
    if (raw.isPrimitive()
        || String.class.equals(raw)
        || Number.class.isAssignableFrom(raw)
        || Boolean.class.equals(raw)
        || Character.class.equals(raw)) {
      return null;
    }

    TypeAdapter<JsonObject> jsonObjectAdapter = gson.getAdapter(JsonObject.class);
    TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      private final char separator = '.';

      /**
       * This function recursively flattens a potentially nested JSON object.
       *
       * <p>It expands upon Marcono1234 proof of concept by instead of combining once, it will given
       * multiple nestings it will recusivly call it self on the child nests.
       *
       * @param name : is the parent key that wants to be combined with its inner children.
       * @param toFlatten : the JSON object that contains all the parent keys children.
       * @param destination : The target JSON object where all flattend keys will be stored into.
       */
      private void flattenInto(
          String name, JsonObject toFlatten, JsonObject destination, boolean isInitial) {
        for (Map.Entry<String, JsonElement> entry : toFlatten.entrySet()) {
          String entrykey = entry.getKey();
          if (entrykey.contains(String.valueOf(separator))) {
            throw new IllegalArgumentException("Unsupported entry key: " + entrykey);
          }
          String flattenedName = name + separator + entrykey;
          if (isInitial) {
            flattenedName = entry.getKey();
          }
          //  Nested structure
          if (entry.getValue().isJsonObject()) {
            flattenInto(flattenedName, entry.getValue().getAsJsonObject(), destination, false);
          } else {
            if (destination.has(flattenedName)) {
              throw new IllegalArgumentException("Duplicate name: " + flattenedName);
            }
            destination.add(flattenedName, entry.getValue());
          }
        }
      }

      /**
       * Given a nested JSON structure it will turn it into a flattend version.
       *
       * <p>This function is purely from Marcono1234 proof of concept
       */
      @Override
      public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.skipValue();
          return null;
        }

        JsonObject jsonObject = jsonObjectAdapter.read(in);
        JsonObject flattened = new JsonObject();

        flattenInto("", jsonObject, flattened, true);
        return delegateAdapter.fromJsonTree(flattened);
      }

      /**
       * Given a flattend JSON we will turn it into a nested structure
       *
       * <p>This function has expands upon Marcono1234 proof of concept, by allowing multiple
       * nestings to be able to be unflattend instead of the originall code which only worked for a
       * depth of 1.
       *
       * @param out : the JsonWriter that we will write the JSON output to
       * @param value : value is the object we will unflatten
       * @throws IOException : java.io.IOException; must be caught or declared to be thrown
       */
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }

        JsonObject flattened = (JsonObject) delegateAdapter.toJsonTree(value);
        JsonObject expanded = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : flattened.entrySet()) {
          String name = entry.getKey();
          JsonElement entryValue = entry.getValue();

          // This basically works like a linked list of linked lists, where our root node / dummy
          // node is expanded and we create are
          JsonObject destination = expanded;
          while (true) {
            int separatorIndex = name.indexOf(separator);
            if (separatorIndex == -1) {
              destination.add(name, entryValue);
              break;
            } else {
              String namePrefix = name.substring(0, separatorIndex);
              String nameSuffix = name.substring(separatorIndex + 1);

              JsonObject nestedObject;
              if (destination.has(namePrefix) && destination.get(namePrefix).isJsonObject()) {
                nestedObject = destination.getAsJsonObject(namePrefix);
              } else {
                nestedObject = new JsonObject();
                destination.add(namePrefix, nestedObject);
              }
              destination = nestedObject;
              name = nameSuffix;
            }
          }
        }
        // Finally write the expanded JsonObject to the actual writer
        jsonObjectAdapter.write(out, expanded);
      }
    };
  }
}
