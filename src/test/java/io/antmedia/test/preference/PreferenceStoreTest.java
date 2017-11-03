package io.antmedia.test.preference;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.prefs.Preferences;

import org.junit.Test;

import io.antmedia.datastore.preference.PreferenceStore;

public class PreferenceStoreTest {


	@Test
	public void testStorePreferences() {

		
		try {
			String fullPath = "target/data.properties";
			File f  = new File(fullPath);
			if (f.exists()) {
				Files.delete(f.toPath());
			}

			PreferenceStore dataStore = new PreferenceStore("data.properties");
			dataStore.setFullPath(fullPath);
			assertNull(dataStore.get("data1"));

			dataStore.put("data1", "value1");
			dataStore.put("data2", "value2");
			dataStore.put("data3", "value3");

			assertEquals(dataStore.get("data1"), "value1");

			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data1"), "value1");

			dataStore = new PreferenceStore("data.properties");
			dataStore.setFullPath(fullPath);
			assertNotNull(dataStore.get("data1"));
			assertEquals(dataStore.get("data1"), "value1");
			dataStore.put("data4", "value4");
			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data3"), "value3");

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}

}
