package io.antmedia.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

    private LoggerUtils() {
        //Hide public constructor
    }

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);

    public static void writeToFile(String absolutePath, String content) {
        try {
            File file = new File(absolutePath);
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public static String getFileContent(String path) {
        try {
            byte[] data = Files.readAllBytes(new File(path).toPath());
            return new String(data);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }
}
