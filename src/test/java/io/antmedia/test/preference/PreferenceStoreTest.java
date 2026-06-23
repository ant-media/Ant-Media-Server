package io.antmedia.test.preference;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import io.antmedia.datastore.preference.PreferenceStore;

@Tag("fast")
public class PreferenceStoreTest {


	@Test
	public void testStorePreferences() {

		
		try {
			String fullPath = "target/data.properties";
			File f  = new File(fullPath);
			if (f.exists()) {
				Files.delete(f.toPath());
			}

			PreferenceStore dataStore = new PreferenceStore(fullPath);
			assertNull(dataStore.get("data1"));

			dataStore.put("data1", "value1");
			dataStore.put("data2", "value2");
			dataStore.put("data3", "value3");

			assertEquals(dataStore.get("data1"), "value1");

			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data1"), "value1");

			dataStore = new PreferenceStore(fullPath);
			assertNotNull(dataStore.get("data1"));
			assertEquals(dataStore.get("data1"), "value1");
			dataStore.put("data4", "value4");
			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data3"), "value3");
			
			dataStore.remove("data4");
			
			assertNull(dataStore.get("data4"));
			
			//create store with full path
			dataStore = new PreferenceStore("data.properties");
			
			dataStore.put("data1", "value1");
			
			assertTrue(dataStore.save());
			assertNotNull(dataStore.get("data1"));
			assertEquals( "value1", dataStore.get("data1"));


		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}

}
