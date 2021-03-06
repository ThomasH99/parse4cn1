///*
// * Copyright 2015 Chidiebere Okwudire.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * Original implementation adapted from Thiago Locatelli's Parse4J project
// * (see https://github.com/thiagolocatelli/parse4j)
// */
//
//package com.parse4cn1;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Date;
//import java.util.List;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import com.parse4cn1.command.ParsePostCommand;
//import com.parse4cn1.command.ParseResponse;
//
// Inherited from Parse4J
// TODO: Port to CN1 i.e. review/extend, document, test and release
//public class ParsePush {
//
//    private List<String> channelSet = null;
//    // private ParseQuery<ParseInstallation> query = null;
//    private Date expirationTime = null;
//    private Date pushTime = null;
//    private Long expirationTimeInterval = null;
//    private Boolean pushToIOS = null;
//    private Boolean pushToAndroid = null;
//    private JSONObject pushData = new JSONObject();
//
//    public void setChannel(String channel) {
//        this.channelSet = new ArrayList<String>();
//        this.channelSet.add(channel);
//    }
//
//    public void setChannels(Collection<String> channels) {
//        this.channelSet = new ArrayList<String>();
//        this.channelSet.addAll(channels);
//    }
//
//    public void setPushTime(Date time) {
//        this.pushTime = time;
//    }
//
//    public void setExpirationTime(Date time) {
//        this.expirationTime = time;
//        this.expirationTimeInterval = null;
//    }
//
//    public void setExpirationTimeInterval(long timeInterval) {
//        this.expirationTime = null;
//        this.expirationTimeInterval = Long.valueOf(timeInterval);
//    }
//
//    public void clearExpiration() {
//        this.expirationTime = null;
//        this.expirationTimeInterval = null;
//    }
//
//    public void setMessage(String message) {
//        this.pushData.put("alert", message);
//    }
//
//    public void setBadge(String badge) {
//        if (badge == null || badge.length() == 0) {
//            badge = "Increment";
//        }
//        this.pushData.put("badge", badge);
//    }
//
//    public void setSound(String sound) {
//        this.pushData.put("sound", sound);
//    }
//
//    public void setTitle(String title) {
//        this.pushData.put("title", title);
//    }
//
//    public void setData(String key, String value) {
//        this.pushData.put(key, value);
//    }
//
//    public void send() throws ParseException {
//        ParsePostCommand command = new ParsePostCommand("push");
//        JSONObject requestData = getJSONData();
//        command.setData(requestData);
//        ParseResponse response = command.perform();
//        if (response.isFailed()) {
//            throw response.getException();
//        }
//    }
//
//    public void sendInBackground(String message, List<String> channels) {
//        SendPushInBackgroundThread event = new SendPushInBackgroundThread();
//        ParseExecutor.runInBackground(event);
//    }
//
//    class SendPushInBackgroundThread extends Thread {
//
//        public void run() {
//            try {
//                send();
//            } catch (ParseException e) {
//                //TODO
//            }
//        }
//    }
//
//    private JSONObject getJSONData() {
//        JSONObject data = new JSONObject();
//        data.put("data", this.pushData);
//
//        if (this.channelSet == null) {
//            data.put("channel", "");
//        } else {
//            data.put("channels", new JSONArray(this.channelSet));
//        }
//
//        if (pushTime != null) {
//            data.put("push_time", Parse.encodeDate(pushTime));
//        }
//
//        if (expirationTimeInterval != null) {
//            data.put("expiration_interval", expirationTimeInterval);
//        }
//
//        if (expirationTime != null) {
//            data.put("expiration_time", expirationTime);
//        }
//
//        return data;
//
//    }
//
//}
