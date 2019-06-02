package io.antmedia.test.preference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import io.antmedia.datastore.preference.PreferenceStore;

public class PreferenceStoreTest {

    private static final String VALUE1 = "value1";
    private static final String DATA_PROPERTIES = "data.properties";


	@Test
	public void testStorePreferences() {

		
		try {
			String fullPath = "target/data.properties";
			File f  = new File(fullPath);
			if (f.exists()) {
				Files.delete(f.toPath());
			}

			PreferenceStore dataStore = new PreferenceStore(DATA_PROPERTIES);
			dataStore.setFullPath(fullPath);
			assertNull(dataStore.get("data1"));

			dataStore.put("data1", VALUE1);
			dataStore.put("data2", "value2");
			dataStore.put("data3", "value3");

			assertEquals(dataStore.get("data1"), VALUE1);

			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data1"), VALUE1);

			dataStore = new PreferenceStore(DATA_PROPERTIES);
			dataStore.setFullPath(fullPath);
			assertNotNull(dataStore.get("data1"));
			assertEquals(dataStore.get("data1"), VALUE1);
			dataStore.put("data4", "value4");
			assertTrue(dataStore.save());

			assertEquals(dataStore.get("data3"), "value3");
			
			dataStore.remove("data4");
			
			assertNull(dataStore.get("data4"));
			
			//create store with full path
			dataStore = new PreferenceStore(DATA_PROPERTIES, true);
			
			dataStore.put("data1", VALUE1);
			
			assertTrue(dataStore.save());
			assertNotNull(dataStore.get("data1"));
			assertEquals( VALUE1, dataStore.get("data1"));


		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}

}
