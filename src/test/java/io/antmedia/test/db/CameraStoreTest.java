package io.antmedia.test.db;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.antmedia.ipcamera.utils.CameraStore;

public class CameraStoreTest {

	@Before
	public void before() {
		deleteMapDBFile();

	}

	@After
	public void after() {
		deleteMapDBFile();
	}

	public void deleteMapDBFile() {
		File f = new File(CameraStore.CAMERA_STORAGE_FILE);
		if (f.exists()) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}

	public void testDbOperations() {

	}

	@Test
	public void testDeleteCameraBug() {

		// These tests are avoided because, there is no seperated camera store
		// and camera db anymore, they are also saved in same db
		
		
		
		
		/*
		 * CameraStore cameraStore = new CameraStore();
		 * 
		 * Camera[] cameraList = cameraStore.getCameraList();
		 * assertNotNull(cameraList);
		 * 
		 * assertEquals(0, cameraList.length);
		 * 
		 * boolean deleteCamera = cameraStore.deleteCamera("1212");
		 * assertFalse(deleteCamera);
		 * 
		 * String ipAddr = "123.334.344.33"; boolean addCamera =
		 * cameraStore.addCamera("name", ipAddr, "username", "password",
		 * "rtspUrl"); assertTrue(addCamera);
		 * 
		 * cameraList = cameraStore.getCameraList(); assertEquals(1,
		 * cameraList.length);
		 * 
		 * deleteCamera = cameraStore.deleteCamera(ipAddr);
		 * assertTrue(deleteCamera);
		 * 
		 * cameraList = cameraStore.getCameraList(); assertEquals(0,
		 * cameraList.length);
		 * 
		 */

	}

}
