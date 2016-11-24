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
package com.parse4cn1.util;

import com.codename1.io.Log;

/**
 * Minimalist file logger.
 * <p>
 * By default, debug logging is disabled and should <em>not</em> not be enabled
 * in production code for performance and security reasons.
 *
 * @author sidiabale
 */
public class Logger {

    private static Logger wrapper;
    private final Log log;
    private int logLevel; //THJ - allow to separate the Parse Log level from a CN1 app's log level

    public static Logger getInstance() {
        if (wrapper == null) {
            wrapper = new Logger();
        }
        return wrapper;
    }

    private Logger() {
        log = Log.getInstance();
//        setLogLevel(Log.INFO);
        setLogLevel(Log.ERROR); //THJ: use minimal logging be default (production use)
//        log.setFileURL("log.txt"); //THJ wrong use of URL, overrides default correct value set by CN1 in Log.java
    }

    public boolean isDebugEnabled() {
        return (Log.getLevel() == Log.DEBUG);
    }

    public final void setLogLevel(int logLevel) {
//        Log.setLevel(logLevel);
        this.logLevel = logLevel; //THJ
//        Log.setLevel(logLevel);
        Log.setLevel(logLevel);
    }

    public final int getLogLevel() { //THJ:
        return logLevel; //THJ
    }

    public void debug(String data) {
//        log.p(data, Log.DEBUG);
        log.p(data, logLevel); //THJ
    }

    public void info(String data) {
//        log.p(data, Log.DEBUG);
//        log.p(data, Log.INFO); //THJ: 
        log.p(data, logLevel); //THJ
    }

    public void warn(String data) {
//        log.p(data, Log.DEBUG);
        log.p(data, logLevel); //THJ
    }

    public void error(String data) {
//        log.p(data, Log.ERROR);
        log.p(data, logLevel); //THJ
    }
}
