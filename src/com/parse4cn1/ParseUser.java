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

import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import com.parse4cn1.callback.LoginCallback;
import com.parse4cn1.callback.RequestPasswordResetCallback;
import com.parse4cn1.callback.SignUpCallback;
import com.parse4cn1.command.ParseCommand;
import com.parse4cn1.command.ParseDeleteCommand;
import com.parse4cn1.command.ParseGetCommand;
import com.parse4cn1.command.ParsePostCommand;
import com.parse4cn1.command.ParseResponse;
import com.parse4cn1.util.Logger;
import com.parse4cn1.util.ParseRegistry;

public class ParseUser extends ParseObject {

    private static final Logger LOGGER = Logger.getInstance();
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EMAIL = "email";
    private static final String OBJECT_ID_CURRENT = "me";
    private static final String ENDPOINT_LOGIN = "login";
    private static final String ENDPOINT_LOGOUT = "logout";

    private String password;
    private String sessionToken;

    protected ParseUser() {
        super(ParseConstants.ENDPOINT_USERS);
    }

    @Override
    public void deleteField(String key) {
        if (KEY_USERNAME.equals(key)) {
            LOGGER.error("Can't remove the username key.");
            throw new IllegalArgumentException("Can't remove the username key.");
        }

        super.deleteField(key);
    }

    @Override
    public void delete() throws ParseException {
        if (!isAuthenticated()) {
            LOGGER.error("Cannot delete a ParseUser that is not authenticated.");
            throw new ParseException(ParseException.SESSION_MISSING,
                    "Cannot delete a ParseUser that is not authenticated.");
        }
        
        ParseCommand command = new ParseDeleteCommand(getEndPoint(), getObjectId());
        command.addHeader(ParseConstants.HEADER_SESSION_TOKEN, getSessionToken());
        ParseResponse response = command.perform();
        if (response.isFailed()) {
            throw response.getException();
        }
        
        reset();
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void setUsername(String username) {
        put(KEY_USERNAME, username);
    }

    public String getUsername() {
        return getString(KEY_USERNAME);
    }

    public void setPassword(String password) {
        this.password = password;
        isDirty = true;
    }

    public void setEmail(String email) {
        put(KEY_EMAIL, email);
    }

    public String getEmail() {
        return getString(KEY_EMAIL);
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public static ParseUser create(String username, String password) throws ParseException {
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
    
    public static ParseUser fetchBySession(final String sessionToken) throws ParseException {
        ParseUser user = null;
        ParseCommand command = 
            new ParseGetCommand(ParseConstants.ENDPOINT_USERS, OBJECT_ID_CURRENT);
        command.addHeader(ParseConstants.HEADER_SESSION_TOKEN, sessionToken);
    
        ParseResponse response = command.perform();
        if (!response.isFailed()) {
            JSONObject jsonResponse = response.getJsonObject();
            if (jsonResponse == null) {
                LOGGER.error("Empty response.");
                throw response.getException();
            }
            
            user = new ParseUser();
            user.setData(jsonResponse);
        } else {
            LOGGER.error("Request failed.");
            throw response.getException();
        }
        return user;
    }

    public static void requestPasswordReset(String email) throws ParseException {

        try {
            ParsePostCommand command = new ParsePostCommand("requestPasswordReset");
            JSONObject data = new JSONObject();
            data.put(KEY_EMAIL, email);
            command.setData(data);
            ParseResponse response = command.perform();
            if (!response.isFailed()) {
                JSONObject jsonResponse = response.getJsonObject();
                if (jsonResponse == null) {
                    LOGGER.error("Empty response.");
                    throw response.getException();
                }
            } else {
                LOGGER.error("Request failed.");
                throw response.getException();
            }
        } catch (JSONException ex) {
            throw new ParseException(ex);
        }
    }
    
    public static void requestPasswordResetInBackground(String email,
        RequestPasswordResetCallback callback) {
        throw new RuntimeException("Not implemented");
    }
    
    public boolean isAuthenticated() {
        return (getSessionToken() != null && getObjectId() != null);
    }
    
    public void signUp() throws ParseException {

        if ((getUsername() == null) || (getUsername().length() == 0)) {
            LOGGER.error("Username cannot be missing or blank");
            throw new IllegalArgumentException(
                    "Username cannot be missing or blank");
        }

        if (password == null || (password.length() == 0)) {
            LOGGER.error("Password cannot be missing or blank");
            throw new IllegalArgumentException(
                    "Password cannot be missing or blank");
        }

        if (getObjectId() != null) {
            LOGGER.error("Cannot sign up a user that has already signed up.");
            throw new IllegalArgumentException(
                    "Cannot sign up a user that has already signed up.");
        }

        ParsePostCommand command = new ParsePostCommand(getClassName());
        try {
            JSONObject parseData = getParseData();
            parseData.put(KEY_PASSWORD, password);
            command.setData(parseData);
            ParseResponse response = command.perform();
            if (!response.isFailed()) {
                JSONObject jsonResponse = response.getJsonObject();
                if (jsonResponse == null) {
                    LOGGER.error("Empty response");
                    throw response.getException();
                }

                setObjectId(jsonResponse.getString(ParseConstants.FIELD_OBJECT_ID));
                setSessionToken(jsonResponse.getString(ParseConstants.FIELD_SESSION_TOKEN));
                String createdAt = jsonResponse.getString(ParseConstants.FIELD_CREATED_AT);
                setCreatedAt(Parse.parseDate(createdAt));
                setUpdatedAt(Parse.parseDate(createdAt));

            } else {
                LOGGER.error("Request failed.");
                throw response.getException();
            }
        } catch (JSONException e) {
            LOGGER.error("Although Parse reports object successfully saved, the response was invalid.");
            throw new ParseException(
                    ParseException.INVALID_JSON,
                    "Although Parse reports object successfully saved, the response was invalid.",
                    e);
        }
    }

    public void login() throws ParseException {

        ParseGetCommand command = new ParseGetCommand(ENDPOINT_LOGIN);
        command.addJson(false);
        command.put(KEY_USERNAME, getUsername());
        command.put(KEY_PASSWORD, password);
        ParseResponse response = command.perform();
        if (!response.isFailed()) {
            JSONObject jsonResponse = response.getJsonObject();
            if (jsonResponse == null) {
                LOGGER.error("Empty response.");
                throw response.getException();
            }
            try {
                setSessionToken(jsonResponse.getString(ParseConstants.FIELD_SESSION_TOKEN));
                jsonResponse.remove(ParseConstants.FIELD_SESSION_TOKEN);
                setData(jsonResponse);
            } catch (JSONException e) {
                LOGGER.error("Although Parse reports object successfully saved, the response was invalid.");
                throw new ParseException(
                        ParseException.INVALID_JSON,
                        "Although Parse reports object successfully saved, the response was invalid.",
                        e);
            }
        } else {
            LOGGER.error("Request failed.");
            throw response.getException();
        }
    }

    public void logout() throws ParseException {

        if (isAuthenticated()) {
            ParseCommand command = new ParsePostCommand(ENDPOINT_LOGOUT);
            command.addHeader(ParseConstants.HEADER_SESSION_TOKEN, getSessionToken());
            ParseResponse response = command.perform();
            if (response.isFailed()) {
                throw response.getException();
            }
            setSessionToken(null);
        }
    }

    public void signUpInBackground(SignUpCallback callback) {
        throw new RuntimeException("Not implemented");
    }

    public static void loginInBackground(String username, String password,
            LoginCallback callback) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void setData(JSONObject jsonObject, boolean disableChecks) {
        if (jsonObject.has(ParseConstants.FIELD_SESSION_TOKEN)) {
            setSessionToken(jsonObject.optString(ParseConstants.FIELD_SESSION_TOKEN));
            jsonObject.remove(ParseConstants.FIELD_SESSION_TOKEN);
        }
        super.setData(jsonObject, disableChecks);
    }
    
    @Override
    protected void setEndPoint(String endPoint) {
        // Prevent any changes to the endpoint
        super.setEndPoint(ParseConstants.ENDPOINT_USERS);
    }

    @Override
    protected void validateSave() throws ParseException {

        if (getObjectId() == null) {
            LOGGER.error("Cannot save a ParseUser until it has been signed up. Call signUp first.");
            throw new ParseException(ParseException.MISSING_OBJECT_ID,
                    "Cannot save a ParseUser until it has been signed up. Call signUp first.");
        }

        if ((!isAuthenticated()) && isDirty && getObjectId() != null) {
            LOGGER.error("Cannot save a ParseUser that is not authenticated.");
            throw new ParseException(ParseException.SESSION_MISSING,
                    "Cannot save a ParseUser that is not authenticated.");
        }
    }

    @Override
    protected void performSave(final ParseCommand command) throws ParseException {
        command.addHeader(ParseConstants.HEADER_SESSION_TOKEN, getSessionToken());
        super.performSave(command);
    }
}