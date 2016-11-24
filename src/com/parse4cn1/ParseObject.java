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
 *
 * Original implementation adapted from Thiago Locatelli's Parse4J project
 * (see https://github.com/thiagolocatelli/parse4j)
 */
package com.parse4cn1;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import com.parse4cn1.Parse.IPersistable;
import com.parse4cn1.callback.GetCallback;
import com.parse4cn1.callback.ParseCallback;
import com.parse4cn1.command.ParseCommand;
import com.parse4cn1.command.ParseDeleteCommand;
import com.parse4cn1.command.ParseGetCommand;
import com.parse4cn1.command.ParsePostCommand;
import com.parse4cn1.command.ParsePutCommand;
import com.parse4cn1.command.ParseResponse;
import com.parse4cn1.encode.PointerEncodingStrategy;
import com.parse4cn1.operation.AddToArrayOperation;
import com.parse4cn1.operation.AddUniqueToArrayOperation;
import com.parse4cn1.operation.DeleteFieldOperation;
import com.parse4cn1.operation.IncrementFieldOperation;
import com.parse4cn1.operation.ParseOperation;
import com.parse4cn1.operation.RemoveFromArrayOperation;
import com.parse4cn1.operation.SetFieldOperation;
import com.parse4cn1.util.Logger;
import com.parse4cn1.encode.ParseDecoder;
import com.parse4cn1.util.ParseRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ParseObject is a local representation of data that can be saved and
 * retrieved from the Parse cloud.
 * <p>
 * The basic workflow for creating new data is to construct a new ParseObject,
 * use {@link #put(java.lang.String, java.lang.Object)} to fill it with data,
 * and then use {@link #save()} to persist to the cloud.
 * <p>
 * The basic workflow for accessing existing data is to use a {@link ParseQuery}
 * to specify which existing data to retrieve.
 */
public class ParseObject implements IPersistable {

    private static final String KEY_ACL = "ACL"; //THJ

    private static final Logger LOGGER = Logger.getInstance();

    private String objectId;
    private String className;
    private String endPoint;
    boolean dirty = false;

    private Map<String, Object> data;
    private Map<String, ParseOperation> operations;
    private List<String> dirtyKeys;

    private Date updatedAt;
    private Date createdAt;

    protected ParseObject(String className) {

        if (className == null) {
            LOGGER.error("You must specify a Parse class name when creating a new ParseObject.");
            throw new IllegalArgumentException(
                    "You must specify a Parse class name when creating a new ParseObject.");
        }

        this.className = className;
        this.data = new Hashtable<String, Object>();
        this.operations = new Hashtable<String, ParseOperation>();
        this.dirtyKeys = new ArrayList<String>();
        setDefaultValues(); //THJ - set default ACL
        setDirty(false); //THJ - hack to avoid that setting the default ACL values above prevent fetchIfNeeded to work (since it tests that an object is not dirty before fetching)
        setEndPoint(toEndPoint(className));
    }

    /**
     * Creates a parse object for the specified class.
     *
     * @param <T> The type of ParseObject to be created.
     * @param className The name of the class associated with this Parse object.
     * @return The newly created Parse object.
     */
    public static <T extends ParseObject> T create(String className) {
        return ParseRegistry.getObjectFactory(className).create(className);
    }

    /**
     * Setter for the object id. In general you do not need to use this.
     * However, in some cases this can be convenient. For example, if you are
     * serializing a ParseObject yourself and wish to recreate it, you can use
     * this to recreate the ParseObject exactly.
     *
     * @param objectId The object ID to set.
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    /**
     * Accessor to the object id. An object id is assigned as soon as an object
     * is saved to the server. The combination of a className and an objectId
     * uniquely identifies an object in your application.
     *
     * @return This Parse object's Id.
     */
    public String getObjectId() {
        return this.objectId;
    }

    /**
     * @return The last time this object was updated on the server.
     */
    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * @return The first time this object was saved on the server.
     */
    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * @return The name of the class associated with this Parse object.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Retrieves the end point associated with this ParseObject. For classes,
     * the endpoint is {@code classes/<className>}.
     *
     * @return The endpoint associated with this object.
     */
    public String getEndPoint() {
        return this.endPoint;
    }

    /**
     * Returns a set view of the keys contained in this object. This does not
     * reserved keys like createdAt, updatedAt or objectId.
     *
     * @return The keys contained in this object.
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.data.keySet());
    }

    /**
     * Retrieves the {@link ParseFile} value associated with {@code key}.
     * <p>
     * This function will not perform a network request. Unless the ParseFile
     * has been downloaded (e.g. by calling {@link ParseFile#getData()}),
     * {@link ParseFile#isDataAvailable()} will return {@code false}.
     *
     * @param key The key associated with the file.
     * @return The retrieved file or null if there is no such {@code key} or if
     * it is not associated with a ParseFile.
     */
    public ParseFile getParseFile(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);
        if (!(value instanceof ParseFile)) {
            logGetValueError("getParseFile", key, value);
            return null;
        }
        return (ParseFile) value;
    }

    /**
     * Retrieves the {@link ParseGeoPoint} value associated with {@code key}.
     *
     *
     * @param key The key associated with the geo point.
     * @return The retrieved point or null if there is no such {@code key} or if
     * it is not associated with a ParseGeoPoint.
     */
    public ParseGeoPoint getParseGeoPoint(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);
        if (!(value instanceof ParseGeoPoint)) {
            logGetValueError("getParseGeoPoint", key, value);
            return null;
        }
        return (ParseGeoPoint) value;
    }

    /**
     * Retrieves the {@link Date} value associated with {@code key}.
     *
     *
     * @param key The key associated with the date value.
     * @return The retrieved date or null if there is no such {@code key} or if
     * it is not associated with a Date.
     */
    public Date getDate(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);
        if (!(value instanceof Date)) {
            logGetValueError("getDate", key, value);
            return null;
        }
        return (Date) value;
    }

    /**
     * Retrieves the {@link Boolean} value associated with {@code key}.
     *
     *
     * @param key The key associated with the boolean value.
     * @return The retrieved boolean value or null if there is no such
     * {@code key} or if it is not associated with a Boolean.
     */
    public Boolean getBoolean(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }

        Object value = this.data.get(key);
        if (!(value instanceof Boolean)) {
            logGetValueError("getBoolean", key, value);
            return null;
        }
        return (Boolean) value;
    }

    /**
     * Retrieves the {@link Integer} value associated with {@code key}.
     *
     *
     * @param key The key associated with the integer value.
     * @return The retrieved integer value or null if there is no such
     * {@code key} or if it is not associated with a Integer.
     */
    public Integer getInt(String key) {
        if (!this.data.containsKey(key)) {
            return null;
        }

        Object value = this.data.get(key);
        if (!(value instanceof Integer)) {
            logGetValueError("getInt", key, value);
            return null;
        }
        return (Integer) value;
    }

    /**
     * Retrieves the {@link Double} value associated with {@code key}.
     *
     *
     * @param key The key associated with the double value.
     * @return The retrieved boolean value or null if there is no such
     * {@code key} or if it is not associated with a Double.
     */
    public Double getDouble(String key) {
        if (!this.data.containsKey(key)) {
            return null;
        }

        Object value = this.data.get(key);
//        if (!(value instanceof Double) {
        if (!(value instanceof Double || value instanceof Integer)) { //THJ: needed otherwise numbers without '.' will not get returned as Double
            logGetValueError("getDouble", key, value);
            return null;
        }
        if (value instanceof Integer) return ((Integer) value).doubleValue(); //THJ
        return (Double) value;
    }

    /**
     * Retrieves the {@link Long} value associated with {@code key}.
     *
     *
     * @param key The key associated with the long value.
     * @return The retrieved boolean value or null if there is no such
     * {@code key} or if it is not associated with a Long.
     */
    public Long getLong(String key) {
        if (!this.data.containsKey(key)) {
            return null;
        }

        Object value = this.data.get(key);
//        if (!(value instanceof Long)) {
        if (!(value instanceof Long || value instanceof Integer)) { //THJ: Parse automatically stores small numbers as Integer
            logGetValueError("getLong", key, value);
            return null;
        }
        return value instanceof Integer ? new Long((Integer) value) : (Long) value; //THJ: necessary since cannot cast int to long
    }

    /**
     * Retrieves the {@link String} value associated with {@code key}.
     *
     *
     * @param key The key associated with the string value.
     * @return The retrieved boolean value or null if there is no such
     * {@code key} or if it is not associated with a String.
     */
    public String getString(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);
        if (!(value instanceof String)) {
            logGetValueError("getString", key, value);
            return null;
        }
        return (String) value;
    }

    /**
     * Retrieves the list or JSONArray value associated with {@code key}.
     *
     * @param <T> The type of the list elements.
     * @param key The key associated with the list value.
     * @return The retrieved list or null if there is no such {@code key} or if
     * the value cannot be converted to a list.
     */
    public <T> List<T> getList(String key) {

        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);

        if ((value instanceof JSONArray)) {
            value = ParseDecoder.decode(value);
            put(key, value);
        }

        if (!(value instanceof List)) {
            return null;
        }

        List<T> returnValue = (List<T>) value;
        return returnValue;
    }

    /**
     * Retrieves the {@link ParseObject} value associated with {@code key}.
     * <p>
     * This function will not perform a network request. Unless the ParseObject
     * has been downloaded (e.g. by a
     * {@link ParseQuery#include(java.lang.String)} or by calling
     * {@link #fetchIfNeeded()}), {@link #isDataAvailable()} will return false.
     *
     * @param key The key associated with the Parse object.
     * @return The retrieved Parse object or null if there is no such
     * {@code key} or if it is not associated with a ParseObject.
     */
    public ParseObject getParseObject(String key) {
        if (!this.data.containsKey(key)) {
            return null;
        }
        Object value = this.data.get(key);
        if (!(value instanceof ParseObject)) {
            logGetValueError("getParseObject", key, value);
            return null;
        }
        return (ParseObject) value;
    }

    /**
     * Retrieves the object value associated with {@code key}.
     *
     *
     * @param key The key associated with the object.
     * @return The retrieved object or null if there is no such {@code key}.
     */
    public Object get(String key) {

        if (key.equals(KEY_ACL)) { //THJ
            return getACL();
        }

        if (!this.data.containsKey(key)) {
            return null;
        }

        Object value = this.data.get(key);
        return value;
    }

    /**
     * Creates a ParseRelation object for defining/updating relations associated
     * with {@code key}.
     *
     * <p>
     * <b>Note:</b> Relations defined via the returned object will override any
     * existing relations previously defined for the same {@code key}!</p>
     *
     * @param <T> A {@link ParseObject} or its sub-type.
     * @param key The key associated with this relation.
     * @return The newly created object.
     */
    public <T extends ParseObject> ParseRelation<T> getRelation(String key) {
        String targetClass = null;
        if (has(key)) {
            targetClass = ((ParseRelation<T>) get(key)).getTargetClass();
        }
        return new ParseRelation<T>(this, key, targetClass);
    }

    /**
     * Checks if the specified {@code key} is defined for this Parse object.
     *
     * @param key The key to be checked.
     * @return {@code true} if {@code key} is defined for this object.
     */
    public boolean has(String key) {
        return containsKey(key);
    }

    /**
     * @see #has(java.lang.String)
     */
    public boolean containsKey(String key) {
        return this.data.containsKey(key);
    }

    /**
     * Checks if this ParseObject has the same class name and objectId as this
     * one.
     *
     * @param other The ParseObject to compare this one to.
     * @return {@code true} if the {@code other} is the same as this Parse
     * object.
     */
    public boolean hasSameId(ParseObject other) {
        return (getClassName() != null) && (getObjectId() != null)
                && (getClassName().equals(other.getClassName()))
                && (getObjectId().equals(other.getObjectId()));
    }

    /**
     * Atomically adds an object to the end of the array associated with a given
     * {@code key}.
     * <p>
     * <b>Note:</b> The corresponding method in the Parse Java SDK is called
     * <a href='http://www.parse.com/docs/android/api/com/parse/ParseObject.html#add(java.lang.String,%20java.lang.Object)'>add</a>.
     *
     * @param key The array field key.
     * @param value The object to add.
     */
    public void addToArrayField(String key, Object value) {
        addAllToArrayField(key, Arrays.asList(value));
    }

    /**
     * Atomically adds the objects contained in {@code values} to the end of the
     * array associated with a given {@code key}.
     * <p>
     * <b>Note:</b> The corresponding method in the Parse Java SDK is called
     * <a href='http://www.parse.com/docs/android/api/com/parse/ParseObject.html#addAll(java.lang.String,%20java.util.Collection)'>addAll</a>.
     *
     * @param key The array field key.
     * @param values The objects to add.
     */
    public void addAllToArrayField(String key, Collection<?> values) {
        AddToArrayOperation operation = new AddToArrayOperation(values);
        performOperation(key, operation);
    }

    /**
     * Atomically adds an object to the array associated with a given key, only
     * if it is not already present in the array. The position of the insert is
     * not guaranteed.
     * <p>
     * <b>Note:</b> The corresponding method in the Parse Java SDK is called
     * <a href='http://www.parse.com/docs/android/api/com/parse/ParseObject.html#addUnique(java.lang.String,%20java.lang.Object)'>addUnique</a>.
     *
     * @param key The array field key.
     * @param value The object to add.
     */
    public void addUniqueToArrayField(String key, Object value) {
        addAllUniqueToArrayField(key, Arrays.asList(value));
    }

    /**
     * Atomically adds the objects contained in {@code values} to the array
     * associated with a given key, only if it is not already present in the
     * array. The position of the insert is not guaranteed.
     * <p>
     * <b>Note:</b> The corresponding method in the Parse Java SDK is called
     * <a href='http://www.parse.com/docs/android/api/com/parse/ParseObject.html#addAllUnique(java.lang.String,%20java.util.Collection)'>addAllUnique</a>.
     *
     * @param key The array field key.
     * @param values The objects to add.
     */
    public void addAllUniqueToArrayField(String key, Collection<?> values) {
        AddUniqueToArrayOperation operation = new AddUniqueToArrayOperation(values);
        performOperation(key, operation);
    }

    /**
     * Atomically removes an object to the end of the array associated with a
     * given {@code key}.
     *
     * @param key The array field key.
     * @param value The object to remove.
     */
    public void removeFromArrayField(String key, Object value) {
        removeAllFromArrayField(key, Arrays.asList(value));
    }

    /**
     * Atomically removes all instances of the objects contained in
     * {@code values} from the array associated with a given key.
     * <p>
     * <b>Note:</b> The corresponding method in the Parse Java SDK is called
     * <a href='http://www.parse.com/docs/android/api/com/parse/ParseObject.html#removeAll(java.lang.String,%20java.util.Collection)'>removeAll</a>.
     *
     * @param key The array field key.
     * @param values The objects to remove.
     */
    public void removeAllFromArrayField(String key, Collection<?> values) {
        RemoveFromArrayOperation operation = new RemoveFromArrayOperation(values);
        performOperation(key, operation);
    }

    public void put(String key, Object value) {

        if (key == null) {
            LOGGER.error("key may not be null.");
            throw new IllegalArgumentException("key may not be null.");
        }

        if (value == null) {
            LOGGER.error("value may not be null.");
            throw new IllegalArgumentException("value may not be null.");
        }

        //THJ: seems the below outcommented check is too strict, the error message exists only for files in Parse
//        if (value instanceof IPersistable && ((IPersistable) value).isDirty()) {
//            LOGGER.error("Persistable object must be saved before being set on a ParseObject.");
//            throw new IllegalArgumentException(
//                    "Persistable object must be saved before being set on a ParseObject.");
//        }
        if (Parse.isReservedKey(key)) {
            LOGGER.error("reserved value for key: " + key);
            throw new IllegalArgumentException("reserved value for key: "
                    + key);
        }

        if (!Parse.isValidType(value)) {
            LOGGER.error("invalid type for value: " + value.getClass().toString());
            throw new IllegalArgumentException("invalid type for value: "
                    + value.getClass().toString());
        }

        //THJ - lines below an optimization to avoid saving objects that have not changed - ERROR doesn't work since euqals first compares for object identify
//        if (value.equals(get(key))) {
//            return;
//        }

        //THJ - lines below an optimization to avoid saving Strings to Parse server when the string value has not changed
        if (value instanceof String) {
            Object oldValue = get(key);
            if (oldValue != null && oldValue instanceof String && ((String) value).equals((oldValue))) {
                return;
            }
        }

        //THJ - lines below an optimization to avoid saving Lists to Parse server when the content (the objects and their sequence) has not changed
        //THJ: this optimizaiton doesn't work since the same list instance is used, instead optimize by only calling put() the list if actually changed
//        if (value instanceof List) {
//            Object oldValue = get(key);
//            if (oldValue != null && oldValue instanceof List) {
//                List oldList = (List) oldValue;
//                List newList = (List) value;
//                if (oldList.size() == newList.size()) {
//                    //if same size, compare the elements:
//                    boolean noChange = true;
//                    for (int i = 0, size = oldList.size(); i < size; i++) {
//                        if (!oldList.get(i).equals(newList.get(i))) {
//                            noChange = false;
//                            break; //exit loop on first difference
//                        }
//                    }
//                    if (noChange) {
//                        return; //all elements equal so don't save
//                    }
//                } //else: if different size, continue to performOperation
//            }
//        }

        performOperation(key, new SetFieldOperation(value));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if any key-value pair in this object (or its
     * children) has been added/updated/removed and not saved yet.
     */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDataAvailable() {
//        return (data != null) && (!data.isEmpty()); //THJ
        return ((data != null) && (!data.isEmpty())) && !((data.size()==1 && data.get(KEY_ACL)!=null)); //THJ - hack to avoid that seeting default values prevent fetchIfNeeded to fetch data
    }

    @Override
    public void save() throws ParseException {

        if (!isDirty() && objectId!=null) { //THJ: added "&& objectId!=null" otherwise a new created object is not saved, which may be necessary to create relation to it from another object
            Logger.getInstance().warn("Ignoring request to save unchanged/empty"
                    + " object");
            return;
        }

        validateSave();

        ParseCommand command;
        if (objectId == null) {
            command = new ParsePostCommand(getEndPoint());
        } else {
            command = new ParsePutCommand(getEndPoint(), getObjectId());
        }

        performSave(command);
    }

    /**
     * Removes a key from this object's data if it exists.
     *
     * @param key The key to be removed.
     */
    public void remove(String key) {

        if (has(key)) {
            if (objectId != null) {
                // if the object was saved before, we need to add the delete operation
                operations.put(key, new DeleteFieldOperation());
            } else {
                operations.remove(key);
            }
            data.remove(key);
            dirtyKeys.add(key);
            setDirty(true);
        }
    }

    /**
     * Atomically decrements the number field associated with {@code key} by 1.
     *
     * @param key The key of the number field to decrement.
     */
    public void decrement(String key) {
        increment(key, -1);
    }

    /**
     * Atomically increments the number field associated with {@code key} by 1.
     *
     * @param key The key of the number field to increment.
     */
    public void increment(String key) {
        increment(key, 1);
    }

    /**
     * Atomically increments the number field associated with {@code key} by the
     * stated {@code amount}.
     *
     * @param key The key of the number field to increment.
     * @param amount The amount to increment the key's value.
     */
    public void increment(String key, Object amount) {
        IncrementFieldOperation operation = new IncrementFieldOperation(amount);
        Object oldValue = data.get(key);
        Object newValue;
        try {
            newValue = operation.apply(oldValue, this, key);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
        data.put(key, newValue);
        operations.put(key, operation);
        dirtyKeys.add(key);
        setDirty(true);
    }

    /**
     * Deletes this object on the server.
     *
     * @throws ParseException if anything goes wrong.
     */
    public void delete() throws ParseException {

        if (getObjectId() == null) {
            LOGGER.error("Attempting to delete an object without an objectId.");
            throw new ParseException(ParseException.MISSING_OBJECT_ID,
                    "Attempting to delete an object without an objectId.");
        }

        ParseCommand command = new ParseDeleteCommand(getEndPoint(), getObjectId());
        ParseResponse response = command.perform();
        if (response.isFailed()) {
            throw response.getException();
        }

        reset();
    }

    /**
     * Encodes the data present in this object in a JSONObject that complies to
     * the Parse API specification.
     *
     * @return The JSON equivalent of this object as expected by Parse.
     * @throws ParseException If anything goes wrong.
     */
    public JSONObject getParseData() throws ParseException {
        JSONObject parseData = new JSONObject();

        for (String key : operations.keySet()) {
            ParseOperation operation = (ParseOperation) operations.get(key);
            try {
                parseData.put(key, operation.encode(PointerEncodingStrategy.get()));
            } catch (JSONException ex) {
                throw new ParseException(ParseException.INVALID_JSON, ParseException.ERR_PROCESSING_RESPONSE, ex);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parseData-> " + parseData);
        }

        return parseData;
    }

    /**
     * Checks the validity of this object's state just before a save operation
     * is performed. Sub-classes should override this method to implement class-
     * specific validation.
     *
     * @throws ParseException if anything goes wrong.
     */
    protected void validateSave() throws ParseException {
    }

    /**
     * Saves this object.
     *
     * @param command The ParseCommand to be used to issue the save request.
     * @throws ParseException if anything goes wrong.
     */
    protected void performSave(final ParseCommand command) throws ParseException {

        command.setMessageBody(getParseData());
        ParseResponse response = command.perform();
        if (!response.isFailed()) {
            JSONObject jsonResponse = response.getJsonObject();
            if (jsonResponse == null) {
                LOGGER.error("Empty response");
                throw response.getException();
            }

            setData(jsonResponse);
            if (getUpdatedAt() == null) {
                setUpdatedAt(getCreatedAt());
            }
        } else {
            LOGGER.error("Request failed.");
            throw response.getException();
        }
    }

    /**
     * Performs the specified ParseOperation on this object.
     *
     * @param key The field to which the result of {@code operation} will be
     * stored, if application.
     * @param operation The ParseOperation to be performed.
     */
    void performOperation(String key, ParseOperation operation) {

        Object oldValue = null;
        if (has(key)) {
            oldValue = data.get(key);
            operations.remove(key);
            data.remove(key);
        }

        Object newValue = null;
        try {
            newValue = operation.apply(oldValue, this, key);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }

        if (newValue != null) {
            data.put(key, newValue);
        } else {
            data.remove(key);
        }
        operations.put(key, operation);
        dirtyKeys.add(key);
        setDirty(true);
    }

    /**
     * Fetches this object with the data from the server. Call this whenever you
     * want the state of the object to reflect exactly what is on the server.
     *
     * @param <T> The concrete type of ParseObject to be fetched.
     * @param className The name of the class associated with this Parse object.
     * @param objectId The id of the object to be fetched. This is the same id
     * that was returned from the server when the object was created.
     * @return The ParseObject that was fetched.
     *
     * @throws ParseException if anything goes wrong.
     */
    public static <T extends ParseObject> T fetch(final String className,
            final String objectId) throws ParseException {

        ParseGetCommand command
                = new ParseGetCommand(toEndPoint(className), objectId);
        ParseResponse response = command.perform();
        if (!response.isFailed()) {
            JSONObject jsonResponse = response.getJsonObject();
            if (jsonResponse == null) {
                throw response.getException();
            }

            T obj = ParseRegistry.getObjectFactory(className).create(className);
            obj.setData(jsonResponse);
            obj.setEndPoint(toEndPoint(className));
            return obj;

        } else {
            throw response.getException();
        }
    }

    /**
     * Fetches this object's data from the server if it has not been fetched
     * (i.e. {@link #isDataAvailable()} and {@link #isDirty()} both return
     * false).
     *
     * @param <T> The concrete type of ParseObject to be fetched.
     * @return The ParseObject that was fetched or this object if the criteria
     * for fetching are not met.
     *
     * @throws ParseException if anything goes wrong.
     */
    public <T extends ParseObject> T fetchIfNeeded() throws ParseException {
        //TODO when called on a just created Item, fetches all Items in Parse - test on objectID!=null before fetchin??
        if (objectId!=null && !isDataAvailable() && !isDirty()) {
//            return fetch(getEndPoint(), getObjectId()); //THJ - error returns a new ParseObject instead of updating the existing one
//            T temp = fetch(getClassName(), getObjectId()); //THJ - only call with classNane since featch adds path
            //THJ - below lines copied from fetch()
            ParseGetCommand command
                    = new ParseGetCommand(toEndPoint(className), objectId);
            ParseResponse response = command.perform();
            if (!response.isFailed()) {
                JSONObject jsonResponse = response.getJsonObject();
                if (jsonResponse == null) {
                    throw response.getException();
                }

//            T obj = ParseRegistry.getObjectFactory(className).create(className);
                setData(jsonResponse);
                setEndPoint(toEndPoint(className));

            } else {
                throw response.getException();
            }
            //THJ - above lines copied from fetch()

//        } else { //THJ
        } //THJ
        return (T) this;
    }

    /**
     * Same as {@link #fetchIfNeeded()} with the option to get notified when the
     * fetch is completed.
     *
     * @param callback The objects whose
     * {@link GetCallback#done(com.parse4cn1.ParseObject, com.parse4cn1.ParseException)}
     * method is invoked when the fetch operation is completed.
     */
    public final <T extends ParseObject> void fetchIfNeeded(GetCallback<T> callback) {
        ParseException exception = null;
        T object = null;

        try {
            object = fetchIfNeeded();
        } catch (ParseException e) {
            exception = e;
        }

        if (callback != null) {
            callback.done(object, exception);
        }
    }

    /**
     * Sets the data for this ParseObject. This method is typically invoked
     * after this object's data is retrieved from the server.
     *
     * @param jsonObject The JSON object containing the data to be set.
     */
    public void setData(JSONObject jsonObject) {

        Iterator<?> it = jsonObject.keys();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = jsonObject.opt(key);
            if (Parse.isReservedKey(key)) {
                setReservedKey(key, value);
            } else {
                put(key, ParseDecoder.decode(value));
            }
        }

        setDirty(false);
        this.operations.clear();
        this.dirtyKeys.clear();
    }

    /**
     * Resets this ParseObject's state. After invoking this method, the
     * ParseObject state is comparable to a newly constructed ParseObject (see:
     * {@link ParseObject#create(java.lang.String)}).
     */
    protected void reset() {
        updatedAt = null;
        createdAt = null;
        objectId = null;
        setDirty(false);
        dirtyKeys.clear();
        operations.clear();
        data.clear();
    }

    protected void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    protected void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    protected final void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    protected void setReservedKey(String key, Object value) {
        if (ParseConstants.FIELD_OBJECT_ID.equals(key)) {
            setObjectId(value.toString());
        } else if (ParseConstants.FIELD_CREATED_AT.equals(key)) {
            setCreatedAt(Parse.parseDate(value.toString()));
        } else if (ParseConstants.FIELD_UPDATED_AT.equals(key)) {
            setUpdatedAt(Parse.parseDate(value.toString()));
        }
    }

    private static String toEndPoint(final String className) {
        return ParseConstants.CLASSES_PATH + className;
    }

    private void logGetValueError(final String methodName, final String key, final Object value) {
        LOGGER.error("Called " + methodName + "(" + key
                + "') but the value is of class type '"
                + value.getClass() + "'");
    }

/////////////////// THJ added below    
    /* package for tests */ boolean isDataAvailable(String key) {
//    synchronized (mutex) { //THJ
//      return isDataAvailable() || estimatedData.containsKey(key);
        return isDataAvailable() || data.containsKey(key);
//    }
    }

    private void checkGetAccess(String key) {
        if (!isDataAvailable(key)) {
            throw new IllegalStateException(
                    "ParseObject has no data for '" + key + "'. Call fetchIfNeeded() to get the data.");
        }
    }

    /**
     * Access the {@link ParseACL} governing this object.
     */
    public ParseACL getACL() {
        return getACL(true);
    }

    private ParseACL getACL(boolean mayCopy) {
//        synchronized (mutex) {
        checkGetAccess(KEY_ACL);
//            Object acl = estimatedData.get(KEY_ACL);
        Object acl = data.get(KEY_ACL);
        if (acl == null) {
            return null;
        }
        if (!(acl instanceof ParseACL)) {
            throw new RuntimeException("only ACLs can be stored in the ACL key");
        }
        if (mayCopy && ((ParseACL) acl).isShared()) {
            ParseACL copy = ((ParseACL) acl).copy();
//                estimatedData.put(KEY_ACL, copy);
            data.put(KEY_ACL, copy);
            return copy;
        }
        return (ParseACL) acl;
//        }
    }

    /**
     * Set the {@link ParseACL} governing this object.
     */
    public void setACL(ParseACL acl) {
        put(KEY_ACL, acl);
    }

    /**
     * Called when a non-pointer is being created to allow additional
     * initialization to occur.
     */
    void setDefaultValues() {
        if (needsDefaultACL() && ParseACL.getDefaultACL() != null) {
            this.setACL(ParseACL.getDefaultACL());
        }
    }

    /**
     * Determines whether this object should get a default ACL. Override in
     * subclasses to turn off default ACLs.
     */
    boolean needsDefaultACL() {
        return true;
    }

}
