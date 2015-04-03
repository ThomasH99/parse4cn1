/*
 * Copyright 2015 Chidiebere Okwudire.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parse4cn1;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import com.parse4cn1.util.ParseDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author sidiabale
 */
public class ParseObjectTest extends BaseParseTest {
    
    @Override
    public boolean runTest() throws Exception {
//        List<ParseObject> objectsToDelete = new ArrayList<ParseObject>();
//        ParseObject obj = null;
//        
//        testRestApiExample();
//        
//        obj = testCreateObjectExtended();
//        objectsToDelete.add(obj);
//        
//        obj = testUpdateObjectExtended();
//        objectsToDelete.add(obj);
//        
//        testDeleteObjects(objectsToDelete);
        
        return true;
    }
    
    private void testRestApiExample() throws ParseException {
        // Create
        ParseObject gameScore = ParseObject.create("GameScore");
        gameScore.put("score", 1337);
        gameScore.put("playerName", "Sean Plott");
        gameScore.put("cheatMode", false);
        gameScore.save();
        
        // Retrieve
        ParseObject retrieved = ParseObject.fetch(gameScore.getEndPoint(), 
                gameScore.getObjectId());
        assertEqual(1337, gameScore.getInt("score"));
        assertEqual("Sean Plott", gameScore.getString("playerName"));
        assertFalse(gameScore.getBoolean("cheatMode"));
        
        // Update
        retrieved.put("score", 73453);
        retrieved.save();
        assertEqual(73453, retrieved.getInt("score"));
        
        // Increment / decrement
        retrieved.increment("score");
        retrieved.save();
        assertEqual(73454, retrieved.getInt("score"));
        
        // Decrement
        retrieved.increment("score", -4);
        retrieved.save();
        assertEqual(73450, retrieved.getInt("score"));
        
        // Increment non-number field
        try {
            retrieved.increment("playerName");
            retrieved.save();
            assertFalse(true, "Increment should only work on number fields");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().startsWith("You cannot increment a non-number"));
        }
        
        // Array operations (manually)
        List<String> skills = new ArrayList<String>();
        skills.add("flying");
        skills.add("kunfu");
        retrieved.put("skills", skills);
        retrieved.save();
        assertEqual(skills, retrieved.getList("skills"));
        
        // Delete field
        retrieved.deleteField("skills");
        retrieved.save();
        assertNull(retrieved.get("skills"));
        
        // Delete object
        retrieved.delete();
    }
    
    private ParseObject testCreateObjectExtended() throws ParseException, JSONException {
        ParseObject obj = ParseObject.create("Car");
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("brand", "Peugeot");
        data.put("model", "208");
        data.put("nrOfDoors", 4);
        data.put("convertible", false);
        data.put("color", "Red");
        data.put("batchNr", getCurrentTimeInHex());
        
        List<String> pastUsers = new ArrayList<String>();
        pastUsers.add("User 1");
        pastUsers.add("User 2");
        pastUsers.add("User 3");
        data.put("pastUsers", pastUsers);
        
        JSONObject facilities = new JSONObject();
        facilities.put("navigationSystem", true);
        facilities.put("airConditioner", false);
        facilities.put("parkAssist", "parkingSensors");
        data.put("facilities", facilities);
        
        addData(obj, data);
        obj.save();
        
        assertNotNull(obj.getCreatedAt(), "Creation time not set");
        assertNotNull(obj.getObjectId(), "Object ID not set");
        assertEqual(obj.getCreatedAt(), obj.getUpdatedAt(), "Creation time should equal update time for new object");
        
        ParseObject retrieved = ParseObject.fetch(obj.getEndPoint(), obj.getObjectId());
        assertEqual(obj.getCreatedAt(), retrieved.getCreatedAt());
        assertEqual(obj.getUpdatedAt(), retrieved.getUpdatedAt());
        assertEqual(obj.getObjectId(), retrieved.getObjectId());
        checkData(retrieved, data);
        
        return retrieved;
    }
    
    private ParseObject testUpdateObjectExtended() throws ParseException {
        ParseObject obj = ParseObject.create("Kitchen");
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("color", "White");
        data.put("style", "modern");
        data.put("renovationYear", 2006);

        List<String> knifeTypes = new ArrayList<String>();
        knifeTypes.add("Wavy Edge");
        knifeTypes.add("Straight Edge");
        knifeTypes.add("Granton Edge");
        
        HashMap<String, Object> knifeInfo = new HashMap<String, Object>();
        knifeInfo.put("count", knifeTypes.size());
        knifeInfo.put("types", knifeTypes);
        data.put("knives", knifeInfo);
        
        addData(obj, data);
        
        obj.save();
        
        checkData(obj, data);
        ParseObject retrieved = ParseObject.fetch(obj.getEndPoint(), obj.getObjectId());
        checkData(retrieved, data);
        
        data.clear();
        
        // Update existing (both simple and nested - list inside sub-object)
        data.put("renovationYear", 2015);
        HashMap<String, Object> retrievedKnifeInfo = 
                (HashMap<String, Object>) retrieved.get("knives");
        knifeTypes.add("Unknown");
        retrievedKnifeInfo.put("types", knifeTypes);
        data.put("knives", retrievedKnifeInfo);
        
        // Add new
        data.put("floor", "laminate");
        JSONArray appliances = new JSONArray();
        appliances.put("refrigerator");
        appliances.put("electricCooker");
        appliances.put("toaster");
        data.put("appliances", appliances);
        
        addData(retrieved, data);
        
        retrieved.save(); // Update
        checkData(retrieved, data);
        retrieved = ParseObject.fetch(obj.getEndPoint(), obj.getObjectId());
        checkData(retrieved, data);
        
        return retrieved;
    }
    
    private void testDeleteObjects(final List<ParseObject> objectsToDelete) {
        for (ParseObject obj : objectsToDelete) {
            try {
                obj.delete();
            } catch (ParseException ex) {
                assertTrue(false, "Error while deleting " + obj.getEndPoint()
                        + "\nError: " + ex.getMessage());
            }
        }
    }
    
    private void checkData(final ParseObject obj, HashMap<String, Object> data) { 
        for (Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof JSONArray) {
                assertEqual(ParseDecoder.convertJSONArrayToList((JSONArray) entry.getValue()), 
                        obj.getList(entry.getKey()));
            } else if (entry.getValue() instanceof JSONObject) {
                assertEqual(ParseDecoder.convertJSONObjectToMap((JSONObject) entry.getValue()), 
                        obj.get(entry.getKey()));
            } else {
                assertEqual(entry.getValue(), obj.get(entry.getKey()));
            }
        }
    }
    
    private void addData(ParseObject obj, HashMap<String, Object> dataToAdd) {
        for (Entry<String, Object> entry : dataToAdd.entrySet()) {
            obj.put(entry.getKey(), entry.getValue());
        }
    }
    // TODO
    // - Create object with complex types (pointers, relations, etc, etc)
    // - Update object complex (counters, relation, etc)
    // - Retrieve all
    // - include children in retrieval
    
    
//    private ParseObject testCreate
  
    
}
