package io.antmedia.test.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
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
			
			Launcher.setInstanceIdFilePath("target/instanceId");
			
			assertTrue(launcher.notifyShutDown("version", "type"));
			
			assertTrue(launcher.startAnalytic("version", "type"));	
			
			assertTrue(launcher.startHeartBeats("version", "type", 1000));
			

			//wait to make sure timer task ender the block
			Awaitility.await().pollDelay(5,TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
			.pollInterval(1, TimeUnit.SECONDS)
			.until(()->{
				return true;
			});
			
			
			launcher.cancelHeartBeat();
	}
	
	@Test
	public void testLogo() {
		Launcher launcher = new Launcher();
		launcher.setLog(Red5LoggerFactory.getLogger(Launcher.class));
		launcher.printLogo();
		
		AsciiArt aa = new AsciiArt();
		
	}


}
