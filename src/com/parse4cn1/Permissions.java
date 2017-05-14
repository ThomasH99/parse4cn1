package com.parse4cn1;

import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import com.codename1.io.Externalizable;
import com.codename1.io.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Thomas
 */

     public class Permissions implements Externalizable { //THJ

        public static final String CLASS_NAME_PARSE_PERMISSIONS = "ParsePermissions";
        private static final String READ_PERMISSION = "read";
        private static final String WRITE_PERMISSION = "write";

//        private final boolean readPermission;
//        private final boolean writePermission;
        private boolean readPermission; //THJ
        private boolean writePermission; //THJ

        /* package */ Permissions(boolean readPermission, boolean write) {
            this.readPermission = readPermission;
            this.writePermission = write;
        }

        /* package */ Permissions(Permissions permissions) {
            this.readPermission = permissions.readPermission;
            this.writePermission = permissions.writePermission;
        }
        
        public Permissions() {
            
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public void externalize(DataOutputStream out) throws IOException {
            Util.writeObject(this.readPermission, out);
            Util.writeObject(this.writePermission, out);
        }

        @Override
        public void internalize(int version, DataInputStream in) throws IOException {
            this.readPermission = (Boolean) Util.readObject(in);
            this.writePermission = (Boolean) Util.readObject(in);
        }

        @Override
        public String getObjectId() {
            return CLASS_NAME_PARSE_PERMISSIONS;
        }

//        /* package */ JSONObject toJSONObject() { //THJ
//            JSONObject json = new JSONObject();
//
//            try {
//                if (readPermission) {
//                    json.put(READ_PERMISSION, true);
//                }
//                if (writePermission) {
//                    json.put(WRITE_PERMISSION, true);
//                }
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }
//            return json;
//        }
//        /* package */ public JSONObject encode() { //THJ
//            JSONObject json = new JSONObject();
//
//            try {
//                if (readPermission) {
//                    json.put(READ_PERMISSION, true);
//                }
//                if (writePermission) {
//                    json.put(WRITE_PERMISSION, true);
//                }
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }
//            return json;
//        }
        public JSONObject encode() throws ParseException { //THJ copied from parse4cn1.ParseObject.java
            JSONObject parseData = new JSONObject();
            try {
                if (readPermission) {
                    parseData.put(READ_PERMISSION, true);
                }
                if (writePermission) {
                    parseData.put(WRITE_PERMISSION, true);
                }
            } catch (JSONException ex) {
                throw new ParseException(ParseException.INVALID_JSON, ParseException.ERR_PROCESSING_RESPONSE, ex);
            }

//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("parseData-> " + parseData); //THJ: commented
//            }

            return parseData;
        }

        /* package */ boolean getReadPermission() {
            return readPermission;
        }

        /* package */ boolean getWritePermission() {
            return writePermission;
        }

        /* package */ static Permissions createPermissionsFromJSONObject(JSONObject object) {
            boolean read = object.optBoolean(READ_PERMISSION, false);
            boolean write = object.optBoolean(WRITE_PERMISSION, false);
            return new Permissions(read, write);
        }

    }


