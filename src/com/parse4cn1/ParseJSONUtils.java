// Error reading included file Templates/Classes/Templates/Licenses/license-apache20_1.txt
package com.parse4cn1;

import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Thomas
 */


  /**
 * Static utility methods pertaining to {@link JSONObject} and {@link JSONArray} instances.
 */
/** package */ class ParseJSONUtils {

  /**
   * Creates a copy of {@code copyFrom}, excluding the keys from {@code excludes}.
   */
//  public static JSONObject create(JSONObject copyFrom, Collection<String> excludes) { //THJ, not used
//    JSONObject json = new JSONObject();
//    Iterator<String> iterator = copyFrom.keys();
//    while (iterator.hasNext()) {
//      String name = iterator.next();
//      if (excludes.contains(name)) {
//        continue;
//      }
//      try {
//        json.put(name, copyFrom.opt(name));
//      } catch (JSONException e) {
//        // This shouldn't ever happen since it'll only throw if `name` is null
//        throw new RuntimeException(e);
//      }
//    }
//    return json;
//  }

  /**
   * A helper for nonugly iterating over JSONObject keys.
   */
  public static Iterable<String> keys(JSONObject object) {
    final JSONObject finalObject = object;
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return finalObject.keys();
      }
    };
  }

  /**
   * A helper for returning the value mapped by a list of keys, ordered by priority.
   */
  public static int getInt(JSONObject object, List<String> keys) throws JSONException {
    for (String key : keys) {
      try {
        return object.getInt(key);
      } catch (JSONException e) {
        // do nothing
      }
    }
    throw new JSONException("No value for " + keys);
  }
}
