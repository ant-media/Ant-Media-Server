package io.antmedia.test.storage;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.amazonaws.event.ProgressListener;
import com.google.cloud.ByteArray;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.antmedia.storage.GCPStorageClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GCPStorageClientTest {


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAndResetGCPStorage() {
        GCPStorageClient gcpStorageClient = new GCPStorageClient();

        Storage instance = gcpStorageClient.getGCPStorage();
        assertNotNull(instance);
        assertEquals(instance, gcpStorageClient.getGCPStorage()); // Ensure it does not create a new instance
        gcpStorageClient.reset(); // reset storage
        assertNotEquals(instance, gcpStorageClient.getGCPStorage());
    }
    
    @Test
    public void testGetGCPStorage() {
		GCPStorageClient gcpStorageClient = new GCPStorageClient();
		Storage storage = gcpStorageClient.getGCPStorage();
		assertNotNull(storage);
		
		gcpStorageClient.setStorageName("dummy");
        gcpStorageClient.setEnabled(true);
        String key = "test-key";
        
        assertNull(gcpStorageClient.get(key));

	}

    @Test
    public void testDelete() {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());
        Storage storage = mock(Storage.class);
        doReturn(storage).when(gcpStorageClient).getGCPStorage();

        gcpStorageClient.setStorageName("dummy");
        gcpStorageClient.setEnabled(true);
        String key = "test-key";

        gcpStorageClient.delete(key);

        verify(storage, times(1)).delete(anyString(), eq(key));
    }

    @Test
    public void testDeleteFile() throws IOException {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());

        // Create a temporary file
        Path tempFilePath = Files.createTempFile("test", ".tmp");
        File tempFile = tempFilePath.toFile();

        // Ensure the file exists before deletion
        assertTrue("Temporary file should exist before deletion", tempFile.exists());

        // Call the deleteFile method
        gcpStorageClient.deleteFile(tempFile);

        // Ensure the file does not exist after deletion
        assertFalse("Temporary file should not exist after deletion", tempFile.exists());

        gcpStorageClient.deleteFile(new File("non existing"));
        //no need for check

    }

    @Test
    public void testFileExist() {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());
        Storage storage = mock(Storage.class);
        doReturn(storage).when(gcpStorageClient).getGCPStorage();
        assertEquals(storage, gcpStorageClient.getGCPStorage());

        gcpStorageClient.setStorageName("dummy");

        Blob mockBlob = mock(Blob.class);
        when(storage.get(anyString(), anyString())).thenReturn(mockBlob);

        gcpStorageClient.setEnabled(true);
        boolean exists = gcpStorageClient.fileExist("test-key");

        assertTrue(exists);

        gcpStorageClient.setEnabled(false);
        boolean exists2 = gcpStorageClient.fileExist("test-key");

        assertFalse(exists2);
    }

    @Test
    public void testFileNotExist() {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());
        Storage storage = mock(Storage.class);
        doReturn(storage).when(gcpStorageClient).getGCPStorage();

        gcpStorageClient.setEnabled(true);

        when(storage.get(anyString(), eq("test-key"))).thenReturn(null);

        boolean exists = gcpStorageClient.fileExist("test-key");

        assertFalse(exists);
    }

    @Test
    public void testSaveWithFile() throws IOException {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());
        Storage storage = mock(Storage.class);
        doReturn(storage).when(gcpStorageClient).getGCPStorage();

        Blob blob = mock(Blob.class);
        when(storage.create(any(), any(byte[].class))).thenReturn(blob);

        Path tempFilePath = Files.createTempFile("test", ".tmp");
        File tempFile = tempFilePath.toFile();

        gcpStorageClient.setEnabled(true);
        gcpStorageClient.setStorageName("dummy");

        gcpStorageClient.save("test-key", tempFile, true);

        verify(storage, times(1)).create(any(BlobInfo.class), any(byte[].class));
        verify(gcpStorageClient, times(1)).deleteFile(any());
        
        tempFilePath = Files.createTempFile("test", ".tmp");
        tempFile = tempFilePath.toFile();
        gcpStorageClient.save("test-key", tempFile, true, Mockito.mock(ProgressListener.class));
        verify(storage, times(2)).create(any(BlobInfo.class), any(byte[].class));
        verify(gcpStorageClient, times(2)).deleteFile(any());

    }

    @Test
    public void testSaveWithStream() throws IOException {
        GCPStorageClient gcpStorageClient = spy(new GCPStorageClient());
        Storage storage = mock(Storage.class);
        doReturn(storage).when(gcpStorageClient).getGCPStorage();

        Path tempFilePath = Files.createTempFile("test", ".tmp");
        File tempFile = tempFilePath.toFile();

        Blob blob = mock(Blob.class);
        when(storage.createFrom(any(), any(InputStream.class))).thenReturn(blob);

        InputStream is = new FileInputStream(tempFile);

        gcpStorageClient.setEnabled(true);
        gcpStorageClient.setStorageName("dummy");

        gcpStorageClient.save("test-key", is, true);

        verify(storage, times(1)).createFrom(any(BlobInfo.class), eq(is));
    }
}
