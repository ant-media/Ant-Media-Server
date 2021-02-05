package io.antmedia.test.logger;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.logger.LoggerUtils;

public class LoggerUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(LoggerUtilsTest.class);
    private final String testPath = "testPath";
    private final String testContent = "123";

    private void deleteFiles(){
        File file = new File(testPath);
        if(file.exists()){
            boolean deleteResult = file.delete();
            if(!deleteResult){
                logger.info("LoggerUtilsTest file deletion failed");
            }
        }
    }

    @Before
    public void before(){
        deleteFiles();
    }

    @After
    public void after(){
        deleteFiles();
    }

    @Test
    public void writeToFileTest(){
        LoggerUtils.writeToFile(testPath,testContent);
        Assert.assertTrue(new File(testPath).exists());
    }

    @Test
    public void writeToFileExceptionTest(){
        LoggerUtils.writeToFile(testPath,null);
        Assert.assertFalse(new File(testPath).exists());
    }

    @Test
    public void getFileContentTest(){
        LoggerUtils.writeToFile(testPath,testContent);
        String fileContent = LoggerUtils.getFileContent(testPath);
        Assert.assertEquals(testContent,fileContent);
    }

    @Test
    public void getFileContentExceptionTest(){
        LoggerUtils.writeToFile("",testContent);
        String fileContent = LoggerUtils.getFileContent(testPath);
        Assert.assertNull(fileContent);
    }
}
