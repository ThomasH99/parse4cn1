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
import com.parse4cn1.encode.ParseDecoder;
import com.parse4cn1.util.ParseRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author THJ
 * TODO:
 * sign in
 * create and retrieve object
 * check read/write rights
 * sign out
 * retrieve object => fail
 * 
 * Change ACL for existing object
 * 
 * Test use of Roles. 
 * Test default ACL??
 * Make a object public 
 * Test with multiple users
 * Test with limited rights (only read, only write)
 * - write with only read access
 */
public class ParseACLTest extends BaseParseTest {

    private final String classGameScore = "GameScore";
    private final String classPlayer = "Player";
    private final String classCar = "Car";
    private final String classKitchen = "Kitchen";

    private static class CustomParseObject extends ParseObject {

        public static final String CLASS_NAME = "CustomParseObject";
        public static final String CUSTOM_FIELD_NAME = "customField";

        private CustomParseObject() {
            super(CLASS_NAME);
            put(CUSTOM_FIELD_NAME, "ReadOnly");
        }

        public String getCustomField() {
            return getString(CUSTOM_FIELD_NAME);
        }

        @Override
        public void put(String key, Object value) {
            if (CUSTOM_FIELD_NAME.equals(key) && !"ReadOnly".equals(value)) {
                throw new IllegalArgumentException("Field " + CUSTOM_FIELD_NAME
                        + " must have value 'ReadOnly'");
            }
            super.put(key, value);
        }
    }

    @Override
    public boolean runTest() throws Exception {
        testRestApiExample();
        testCreateObjectExtended();
        testUpdateObjectExtended();
        testCustomParseObjectClass();
        return true;
    }

    @Override
    protected void resetClassData() {
        deleteObjects(classGameScore);
        deleteObjects(classPlayer);
        deleteObjects(classCar);
        deleteObjects(classKitchen);
        deleteObjects(CustomParseObject.CLASS_NAME);
    }

    private void testRestApiExample() throws ParseException {
        // Create
        ParseObject gameScore = ParseObject.create(classGameScore);
        gameScore.put("score", 1337);
        gameScore.put("playerName", "Sean Plott");
        gameScore.put("cheatMode", false);
        gameScore.save();

        // Retrieve
        ParseObject retrieved = ParseObject.fetch(gameScore.getClassName(),
                gameScore.getObjectId());
        assertEqual(Integer.valueOf(1337), gameScore.getInt("score"));
        assertEqual("Sean Plott", gameScore.getString("playerName"));
        assertFalse(gameScore.getBoolean("cheatMode"));

        // Update
        retrieved.put("score", 73453);
        retrieved.save();
        assertEqual(Integer.valueOf(73453), retrieved.getInt("score"));

        // Increment / decrement
        retrieved.increment("score");
        retrieved.save();
        assertEqual(Integer.valueOf(73454), retrieved.getInt("score"));

        // Decrement
        retrieved.increment("score", -4);
        retrieved.save();
        assertEqual(Integer.valueOf(73450), retrieved.getInt("score"));

        // Increment non-number field
        try {
            retrieved.increment("playerName");
            retrieved.save();
            assertFalse(true, "Increment should only work on number fields");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().startsWith("You cannot increment a non-number"));
        }

        // Relations
        ParseObject opponent = ParseObject.create(classPlayer);
        opponent.put("playerName", "Sean Plott");
        opponent.save();

        ParseRelation<ParseObject> opponentsRelation = retrieved.getRelation("opponents");
        opponentsRelation.add(opponent);
        retrieved.save();

        ParseRelation<ParseObject> retrievedRelation
                = (ParseRelation<ParseObject>) retrieved.get("opponents");
        assertEqual(retrievedRelation.getTargetClass(), opponent.getClassName());

        retrieved = ParseObject.fetch(retrieved.getClassName(), retrieved.getObjectId());
        retrievedRelation = (ParseRelation<ParseObject>) retrieved.getRelation("opponents");
        assertNotNull(retrievedRelation);
        assertEqual(retrievedRelation.getTargetClass(), opponent.getClassName());
        opponent.delete();

        // Array operations (manually)
        List<String> skills = new ArrayList<String>();
        skills.add("flying");
        skills.add("kunfu");
        retrieved.put("skills", skills);
        retrieved.save();
        assertEqual(skills, retrieved.getList("skills"));

        // Delete field
        retrieved.remove("skills");
        retrieved.save();
        assertNull(retrieved.get("skills"));

        // Array operation ('atomic')
        testArrayOperations(retrieved);

        // Delete object
        retrieved.delete();
    }

    private void testArrayOperations(final ParseObject obj) throws ParseException {
        final String skillBoxing = "boxing";

        List<String> skills = new ArrayList<String>();
        final String skillWrestling = "wrestling";
        final String skillKunfu = "kunfu";

        skills.add("flying");
        skills.add(skillKunfu);
        skills.add(skillKunfu);
        skills.add("running");

        final String fieldSkills = "skills";
        obj.addAllToArrayField(fieldSkills, skills);
        obj.save();
        assertEqual(skills, obj.getList(fieldSkills), "Duplicate 'kunfu' is allowed");

        skills.add(skillBoxing);
        obj.addToArrayField(fieldSkills, skillBoxing);
        obj.save();
        assertEqual(skills, obj.getList(fieldSkills), "Boxing skill is added");

        obj.addUniqueToArrayField(fieldSkills, skillBoxing);
        obj.save();
        assertEqual(skills, obj.getList(fieldSkills), "Duplicate boxing field is not added");

        List<String> extraSkills = Arrays.asList(skillWrestling, skillBoxing);
        obj.addAllUniqueToArrayField(fieldSkills, extraSkills);
        obj.save();
        skills.add(skillWrestling);
        assertEqual(skills, obj.getList(fieldSkills),
                "Only wrestling skill is added; duplicate boxing skill is ignored");

        obj.removeFromArrayField(fieldSkills, skillKunfu);
        obj.save();
        skills.remove(skillKunfu);
        skills.remove(skillKunfu);
        assertEqual(skills, obj.getList(fieldSkills),
                "All kunfu skills are removed");

        obj.removeAllFromArrayField(fieldSkills, extraSkills);
        obj.save();
        skills.removeAll(extraSkills);

        final ParseObject retrieved = ParseObject.fetch(obj.getClassName(), obj.getObjectId());
        assertEqual(skills, retrieved.getList(fieldSkills),
                "All extra skills (" + extraSkills.toString() + ") are removed");
    }

    private void testCreateObjectExtended() throws ParseException, JSONException {
        ParseObject obj = ParseObject.create(classCar);
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

        ParseObject retrieved = ParseObject.fetch(obj.getClassName(), obj.getObjectId());
        assertEqual(obj.getCreatedAt(), retrieved.getCreatedAt());
        assertEqual(obj.getUpdatedAt(), retrieved.getUpdatedAt());
        assertEqual(obj.getObjectId(), retrieved.getObjectId());
        checkData(retrieved, data);
    }

    private void testUpdateObjectExtended() throws ParseException {
        ParseObject obj = ParseObject.create(classKitchen);
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
        ParseObject retrieved = ParseObject.fetch(obj.getClassName(), obj.getObjectId());
        checkData(retrieved, data);

        data.clear();

        // Update existing (both simple and nested - list inside sub-object)
        data.put("renovationYear", 2015);
        HashMap<String, Object> retrievedKnifeInfo
                = (HashMap<String, Object>) retrieved.get("knives");
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
        retrieved = ParseObject.fetch(obj.getClassName(), obj.getObjectId());
        checkData(retrieved, data);
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

    private void testCustomParseObjectClass() throws ParseException {
        try {
            CustomParseObject obj = ParseObject.create(CustomParseObject.CLASS_NAME);
            obj.put("purpose", "test");
            obj.save();
            fail("ClassCastException expected since custom sub-class has not been registered");
        } catch (ClassCastException ex) {
            assertTrue(true);
        }

        ParseRegistry.registerSubclass(CustomParseObject.class, CustomParseObject.CLASS_NAME);
        ParseRegistry.registerParseFactory(CustomParseObject.CLASS_NAME, new Parse.IParseObjectFactory() {

            @Override
            public <T extends ParseObject> T create(String className) {
                if (CustomParseObject.CLASS_NAME.equals(className)) {
                    return (T) new CustomParseObject();
                }
                throw new IllegalArgumentException("Unsupported class name: " + className);
            }
        });

        CustomParseObject obj = null;
        try {
            obj = ParseObject.create(CustomParseObject.CLASS_NAME);
            obj.put("purpose", "test");
            obj.save();
        } catch (Exception ex) {
            fail("No exception expected since custom sub-class has been registered "
                    + "but got exception: " + ex);
        }

        assertNotNull(obj, "Class CustomParseObject should be instantiable");
        final CustomParseObject retrieved = ParseObject.fetch(
                CustomParseObject.CLASS_NAME, obj.getObjectId());
        assertNotNull(retrieved, "Saved custom object should be retrievable");
        assertEqual("test", retrieved.getString("purpose"));
        assertEqual("ReadOnly", retrieved.getString(CustomParseObject.CUSTOM_FIELD_NAME));

    }
}
