/*
 * Autopsy Forensic Browser
 *
 * Copyright 2012-2014 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.coreutils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.LogRecord;
import org.sleuthkit.autopsy.casemodule.Case;

/**
 * Autopsy specialization of the Java Logger class with custom file handlers.
 */
public final class Logger extends java.util.logging.Logger {

    private static final String LOG_ENCODING = PlatformUtil.getLogFileEncoding();
    private static final int LOG_SIZE = 0; // In bytes, zero is unlimited
    private static final int LOG_FILE_COUNT = 10;
    private static final String LOG_WITHOUT_STACK_TRACES = "autopsy.log"; //NON-NLS
    private static final String LOG_WITH_STACK_TRACES = "autopsy_traces.log"; //NON-NLS
    private static final CaseChangeListener caseChangeListener = new CaseChangeListener();
    private static final Handler console = new java.util.logging.ConsoleHandler();
    private static FileHandler userFriendlyLogFile = createFileHandler(PlatformUtil.getLogDirectory(), LOG_WITHOUT_STACK_TRACES);
    private static FileHandler developersLogFile = createFileHandler(PlatformUtil.getLogDirectory(), LOG_WITH_STACK_TRACES);

    static {
        Case.addPropertyChangeListener(caseChangeListener);
    }

    private static class CaseChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(Case.Events.CURRENT_CASE.toString())) {
                // Write to logs in the Logs directory of the current case, or 
                // to logs in the user directory when there is no case.
                if (event.getNewValue() != null) {
                    String logDirectoryPath = ((Case) event.getNewValue()).getLogDirectoryPath();
                    if (!logDirectoryPath.isEmpty()) {
                        userFriendlyLogFile.close();
                        userFriendlyLogFile = createFileHandler(logDirectoryPath, LOG_WITHOUT_STACK_TRACES);
                        developersLogFile.close();
                        developersLogFile = createFileHandler(logDirectoryPath, LOG_WITH_STACK_TRACES);
                    }
                } else {
                    userFriendlyLogFile.close();
                    userFriendlyLogFile = createFileHandler(PlatformUtil.getLogDirectory(), LOG_WITHOUT_STACK_TRACES);
                    developersLogFile.close();
                    developersLogFile = createFileHandler(PlatformUtil.getLogDirectory(), LOG_WITH_STACK_TRACES);
                }
            }
        }
    }

    private static FileHandler createFileHandler(String logDirectory, String fileName) {
        try {
            FileHandler f = new FileHandler(logDirectory + File.separator + fileName, LOG_SIZE, LOG_FILE_COUNT);
            f.setEncoding(LOG_ENCODING);
            switch (fileName) {
                case LOG_WITHOUT_STACK_TRACES:
                    f.setFormatter(new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return (new Date(record.getMillis())).toString() + " : "
                                    + record.getLevel() + ": "
                                    + record.getSourceClassName() + " "
                                    + record.getSourceMethodName() + ": "
                                    + this.formatMessage(record) + "\n";
                        }
                    });
                    break;
                case LOG_WITH_STACK_TRACES:
                    f.setFormatter(new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            if (record.getThrown() != null) {

                                StackTraceElement ele[] = record.getThrown().getStackTrace();
                                String StackTrace = "";
                                for (StackTraceElement ele1 : ele) {
                                    StackTrace += "\t" + ele1.toString() + "\n";
                                }

                                return (new Timestamp(record.getMillis())).toString() + " : "
                                        + record.getLevel() + ": "
                                        + record.getSourceClassName() + " "
                                        + record.getSourceMethodName() + ": "
                                        + this.formatMessage(record) + " "
                                        + record.getThrown().toString() + ": "
                                        + StackTrace
                                        + "\n";
                            } else {
                                return (new Timestamp(record.getMillis())).toString() + " : "
                                        + record.getLevel() + ": "
                                        + record.getSourceClassName() + " "
                                        + record.getSourceMethodName() + ": "
                                        + this.formatMessage(record) + " "
                                        + "\n";
                            }
                        }
                    });
                    break;
            }
            return f;
        } catch (IOException e) {
            throw new RuntimeException("Error initializing " + fileName + " file handler", e); //NON-NLS
        }
    }

    /**
     * Factory method to retrieve a org.sleuthkit.autopsy.coreutils.Logger
     * instance derived from java.util.logging.Logger. Hides the base class
     * factory method.
     *
     * @param name A name for the logger. This should be a dot-separated name
     * and should normally be based on the package name or class name.
     * @return org.sleuthkit.autopsy.coreutils.Logger instance
     */
    public static Logger getLogger(String name) {
        return new Logger(name, null);
    }

    /**
     * Factory method to retrieve a org.sleuthkit.autopsy.coreutils.Logger
     * instance derived from java.util.logging.Logger. Hides the base class
     * factory method.
     *
     * @param name A name for the logger. This should be a dot-separated name
     * and should normally be based on the package name or class name.
     * @param resourceBundleName - name of ResourceBundle to be used for
     * localizing messages for this logger. May be null if none of the messages
     * require localization.
     * @return org.sleuthkit.autopsy.coreutils.Logger instance
     */
    public static Logger getLogger(String name, String resourceBundleName) {
        return new Logger(name, resourceBundleName);
    }

    private Logger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
        if (Version.getBuildType() == Version.Type.DEVELOPMENT) {
            super.addHandler(console);
        }
        super.setUseParentHandlers(false);
        super.addHandler(userFriendlyLogFile);
        super.addHandler(developersLogFile);
    }
}
