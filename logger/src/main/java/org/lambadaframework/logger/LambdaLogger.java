package org.lambadaframework.logger;


import com.amazonaws.services.lambda.runtime.log4j.LambdaAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

public class LambdaLogger {

    private static Appender appender;

    private static Level globalLogLevel = Level.ALL;

    private LambdaLogger() {
    }

    private static Appender getAppender() {
        if (appender == null) {
            PatternLayout patternLayout = new PatternLayout();
            patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss} <%X{AWSRequestId}> %-5p %c{1}:%L - %m%n");
            appender = new LambdaAppender();
            appender.setLayout(patternLayout);
        }
        return appender;
    }

    public static void setLogLevel(Level level) {
        globalLogLevel = level;
    }

    public static org.apache.log4j.Logger getLogger(Class clazz) {
        return getLogger(clazz, globalLogLevel);
    }

    public static org.apache.log4j.Logger getLogger(Class clazz, Level level) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(clazz);
        logger.setLevel(level);
        logger.addAppender(getAppender());
        return logger;
    }
}

