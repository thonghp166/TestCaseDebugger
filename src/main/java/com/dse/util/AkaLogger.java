package com.dse.util;

import com.dse.guifx_v3.helps.UIController;
import org.apache.log4j.Logger;

public class AkaLogger extends Logger {

    protected AkaLogger(String name) {
        super(name);
    }

    public static AkaLogger get(Class<?> c) {
        Logger root = Logger.getRootLogger();

        AkaLogger logger = new AkaLogger(c.getName());

        logger.repository = root.getLoggerRepository();
        logger.parent = root;

        // disable stdout target logger if using command line version
        if (UIController.getPrimaryStage() == null) {
            root.removeAppender("stdout");
        }

        return logger;
    }
}
