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

import java.util.List;

/**
 *
 * @author sidiabale
 */
public class ParseUserTest extends BaseParseTest {

    /*
    TODO: Interesting test cases
    1. Sign up
       - Happy flow [OK]
       - Save before signup 
       - Email verification (setting of field) [OK]
    2. Logging in [OK]
    3. Password reset
       - User without email (should fail)
       - User with email [OK]
    4. Retrieving users
       - By ID [OK]
       - By sessionToken [OK]
       - By user root endpoint (retrieve all)
    5. Updating users
       - Check uniqueness of username and email (nothing is mentioned in API doc
         about changing though I expect that to be possible)
       - Check changing username and/or email
       - Check adding new fields [OK]
    5. Deleting users [OK]
    6. Linking users
       - Facebook
       - Twitter
    7.
    */
    
    @Override
    public boolean runTest() throws Exception {
        
        testRestApiExample();
        testQueryingUsers();
        
        return true;
    }

    @Override
    public void cleanup() {
        deleteAllUsers();
        super.cleanup();
    }
    
    private void testRestApiExample() throws ParseException { 
        deleteAllUsers(); // Avoid failures due to duplicate info
        
        // Create and sign up
        final String username = "user_" + getCurrentTimeInHex();
        final String phone = "phone";
        ParseUser user = ParseUser.create(username, TEST_PASSWORD);
        
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        assertNull(user.getSessionToken());
        assertNull(user.getObjectIdP());
        
        user.put(phone, "415-392-0202");
        user.signUp();
        
        assertNotNull(user.getObjectIdP(), 
                 "Object id should be set upon successful signup");
        assertNotNull(user.getCreatedAt(), 
                "Creation time should be set upon successful signup");
        assertEqual(user.getCreatedAt(), user.getUpdatedAt(), 
                "Update time is set to created time by Parse4CN1");
        assertNotNull(user.getSessionToken(), 
                "Session token should be created upon successful signup");
        
        ParseUser loggedIn = ParseUser.create(username, TEST_PASSWORD);
        loggedIn.login();
        
        assertNotNull(loggedIn.getSessionToken(),
                "Session token is created upon successful login");
        assertEqual(user.getObjectIdP(), loggedIn.getObjectIdP(),
                "Object id is preserved after sign up");
        assertEqual(user.getString(phone), loggedIn.getString(phone),
                "User data is preserved");
        
        final String sessionToken = loggedIn.getSessionToken();
        loggedIn.logout();
        assertNull(loggedIn.getSessionToken(), 
                "Session token is invalidated on logout");
        
        loggedIn.login();
        assertNotNull(loggedIn.getSessionToken(), 
                "Session token is created upon successful re-login");
        assertNotEqual(sessionToken, loggedIn.getSessionToken(), 
            "New session token created upon successful re-login is different "
                    + "from previous one");
        
        // Verify email
        final String emailVerified = "emailVerified";
        final String email = "email";
        assertNull(loggedIn.getBoolean(emailVerified), 
                "emailVerified field should not be defined initially");
        loggedIn.put(email, "test@test.com");
        loggedIn.save();
        assertFalse(loggedIn.getBoolean(emailVerified), 
                emailVerified + " field should be defined but false");
        
        // Retrieve by object id
        ParseUser userById = ParseUser.fetch(loggedIn.getClassName(), loggedIn.getObjectIdP());
        assertEqual(loggedIn.getString(phone),          userById.getString(phone));
        assertEqual(loggedIn.getBoolean(emailVerified), userById.getBoolean(emailVerified));
        assertEqual(loggedIn.getString(email),          userById.getString(email));
        assertEqual(loggedIn.getCreatedAt(),            userById.getCreatedAt());
        assertEqual(loggedIn.getUpdatedAt(),            userById.getUpdatedAt());
        assertNull(userById.getSessionToken(), "Session token is not returned on retrieval by object id");
        
        // Retrieve by sessionToken
        ParseUser userBySession = ParseUser.fetchBySession(loggedIn.getSessionToken());
        assertEqual(loggedIn.getString(phone),          userBySession.getString(phone));
        assertEqual(loggedIn.getBoolean(emailVerified), userBySession.getBoolean(emailVerified));
        assertEqual(loggedIn.getString(email),          userBySession.getString(email));
        assertEqual(loggedIn.getCreatedAt(),            userBySession.getCreatedAt());
        assertEqual(loggedIn.getUpdatedAt(),            userBySession.getUpdatedAt());
        
        // Reset password
        ParseUser.requestPasswordReset("test@test.com");
        
        loggedIn.logout();
    }

    private void testQueryingUsers() throws ParseException {
        deleteAllUsers();
        final int userCount = 5;
        for (int i = 1; i <= userCount; ++i) {
            ParseUser user = ParseUser.create("user" + i, TEST_PASSWORD);
            user.signUp();
        }
        ParseQuery<ParseUser> query = ParseQuery.getQuery(ParseConstants.CLASS_NAME_USER);
        List<ParseUser> results = query.find();
        assertEqual(userCount, results.size(), "All users are returned by query");
    }
}
