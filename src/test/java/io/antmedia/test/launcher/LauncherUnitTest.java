package io.antmedia.test.launcher;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.Launcher;
import org.red5.server.scope.WebScope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AsciiArt;


@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class LauncherUnitTest {
	
	protected WebScope appScope;
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	
	@Test
	public void testAll() {
			Launcher launcher = new Launcher();
			File f = new File("testFile");
			String content = "contentntntnt";
			launcher.writeToFile(f.getAbsolutePath(), content);
			
			String fileContent = launcher.getFileContent(f.getAbsolutePath());
			
			assertEquals(fileContent, content);
			
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
	}
	
	@Test
	public void testLaunch() {
		Launcher launcher = new Launcher();
		assertEquals("Community", Launcher.getVersionType());
		assertEquals("Community", launcher.getVersionType()); //increase coverage
		Launcher.getVersion();
	}
	
	@Test
	public void testLogo() {
		Launcher launcher = new Launcher();
		launcher.setLog(Red5LoggerFactory.getLogger(Launcher.class));
		launcher.printLogo();
		
		AsciiArt aa = new AsciiArt();
		
	}


}
