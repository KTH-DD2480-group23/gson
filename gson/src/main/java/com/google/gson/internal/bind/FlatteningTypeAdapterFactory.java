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
 * Marcono1234 proof of concept, accompanied by a new corresponding test file. Ensuring this file
 * works correctly.
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

      private void flattenInto(String name, JsonObject toFlatten, JsonObject destination) {
        for (Map.Entry<String, JsonElement> entry : toFlatten.entrySet()) {
          String flattenedName = name + separator + entry.getKey();
          if (destination.has(flattenedName)) {
            throw new IllegalArgumentException("Duplicate name: " + flattenedName);
          }
          destination.add(flattenedName, entry.getValue());
        }
      }

      /** !! Own doc Reads Json */
      @Override
      public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.skipValue();
          return null;
        }

        /*
         * Flattens nested JsonObject values, e.g.:
         *   {
         *     "a": 1,
         *     "b": {
         *       "x": true,
         *       "y": 2
         *     }
         *   }
         * Becomes
         *   {
         *     "a": 1,
         *     "b.x": true,
         *     "b.y": 2
         *   }
         */

        JsonObject jsonObject = jsonObjectAdapter.read(in);
        JsonObject flattened = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
          String name = entry.getKey();
          JsonElement value = entry.getValue();

          // Flatten the value
          if (value instanceof JsonObject) {
            flattenInto(name, (JsonObject) value, flattened);
          } else {
            flattened.add(name, value);
          }

          // But also add the non-flattened value in case this entry should not actually be
          // flattened
          // The delegate adapter will then ignore either the flattened or the non-flattened entries
          //   if (flattened.has(name)) {
          //     throw new IllegalArgumentException("Duplicate name: " + name + " and " +
          // flattened);
          //   }
          //   flattened.add(name, value);
        }

        // System.out.println("flattened " + flattened);
        // Now read the flattened JsonObject using the delegate adapter
        return delegateAdapter.fromJsonTree(flattened);
      }

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }

        /*
         * Expands the flattened JsonObject, e.g.:
         *   {
         *     "a": 1,
         *     "b.x": true,
         *     "b.y": 2
         *   }
         * Becomes
         *   {
         *     "a": 1,
         *     "b": {
         *       "x": true,
         *       "y": 2
         *     }
         *   }
         */

        JsonObject flattened = (JsonObject) delegateAdapter.toJsonTree(value);
        JsonObject expanded = new JsonObject();
        Map<String, JsonElement> expandedAsMap = expanded.asMap();

        for (Map.Entry<String, JsonElement> entry : flattened.entrySet()) {
          String name = entry.getKey();
          JsonElement entryValue = entry.getValue();

          // Expand the flattened entry
          int separatorIndex = name.indexOf(separator);
          if (separatorIndex != -1) {
            String namePrefix = name.substring(0, separatorIndex);
            String nameSuffix = name.substring(separatorIndex + 1);
            JsonObject nestedObject =
                (JsonObject) expandedAsMap.computeIfAbsent(namePrefix, k -> new JsonObject());

            if (nestedObject.has(nameSuffix)) {
              throw new IllegalArgumentException("Duplicate name: " + nameSuffix);
            }
            nestedObject.add(nameSuffix, entryValue);
          } else {
            if (expanded.has(name)) {
              throw new IllegalArgumentException("Duplicate name: " + name);
            }
            expanded.add(name, entryValue);
          }
        }

        // Finally write the expanded JsonObject to the actual writer
        jsonObjectAdapter.write(out, expanded);
      }
    };
  }
}
