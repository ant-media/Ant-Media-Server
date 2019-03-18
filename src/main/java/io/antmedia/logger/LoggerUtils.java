package io.antmedia.logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

class LoggerUtils {

    private LoggerUtils() {
        //Hide public constructor
    }

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);

    static void writeToFile(String absolutePath, String content) {
        try {
            File file = new File(absolutePath);
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }

    }

    static String getFileContent(String path) {
        try {
            byte[] data = Files.readAllBytes(new File(path).toPath());
            return new String(data);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }
}
