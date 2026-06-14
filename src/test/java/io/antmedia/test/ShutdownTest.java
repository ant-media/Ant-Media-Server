package io.antmedia.test;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.red5.server.Shutdown;

@Tag("fast")
public class ShutdownTest {
	
	
	private static final String tokenFile = "shutdown.token";
	
	
	@AfterEach
	public void after() {
		try {
			Files.deleteIfExists(new File(tokenFile).toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetToken() {
		
		String token = Shutdown.getToken(null);
		assertNull(token);
		
		
		token = Shutdown.getToken("tst");
		assertEquals("tst", token);
		
		File f = new File(tokenFile);
		
		String randomNumeric = RandomStringUtils.randomNumeric(36);
		try {
			Files.write(f.toPath(), randomNumeric.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
			token = Shutdown.getToken("tst");
			
			assertEquals(token, randomNumeric);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		
		
	}

}
