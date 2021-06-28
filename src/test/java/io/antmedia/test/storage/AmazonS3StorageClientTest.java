package io.antmedia.test.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import java.io.File;

import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;

import io.antmedia.storage.AmazonS3StorageClient;

public class AmazonS3StorageClientTest {

	public final String ACCESS_KEY = "";
	public final String SECRET_KEY = "";
	
	public final String BUCKET_NAME = "";
	
	public final String REGION = "";
	
	//@Test
	public void testS3() {
		AmazonS3StorageClient storage = new AmazonS3StorageClient();
		
		storage.setAccessKey(ACCESS_KEY);
		storage.setSecretKey(SECRET_KEY);
		storage.setRegion(REGION);
		storage.setStorageName(BUCKET_NAME);
		
		File f = new File("src/test/resources/test.flv");
		storage.save("streams" + "/" + f.getName() , f);
	}
	
	@Test
	public void testException() {
		try {
			AmazonS3StorageClient storage = new AmazonS3StorageClient();
		
			storage.delete("streams/" + "any_file");
			
			storage.fileExist("any_file");
			
			storage.fileExist("streams/any_file");
			
			storage.save("streams/any_file", new File("any_file"));
			
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testChangeS3Settings() {
		
		AmazonS3StorageClient storage = spy(new AmazonS3StorageClient());

		//Call getAmazonS3 with default settings
		AmazonS3 amazonS3 = storage.getAmazonS3();
		assertNotNull(amazonS3);
		
		AmazonS3 amazonS3_2 = storage.getAmazonS3();
		assertNotNull(amazonS3_2);
		
		assertEquals(amazonS3_2, amazonS3);
		
		storage.reset();
		AmazonS3 amazonS3_3 = storage.getAmazonS3();
		assertNotEquals(amazonS3_3, amazonS3);
		
	}
	
	@Test
	public void testCannedAcl() {
		AmazonS3StorageClient storage = new AmazonS3StorageClient();
		
		assertEquals(CannedAccessControlList.PublicRead, storage.getCannedAcl());
		
		storage.setPermission("nothing supported");
		
		assertEquals(CannedAccessControlList.PublicRead, storage.getCannedAcl());
		
		storage.setPermission("private");
		assertEquals(CannedAccessControlList.Private, storage.getCannedAcl());
		
		storage.setPermission("public-read-write");
		assertEquals(CannedAccessControlList.PublicReadWrite, storage.getCannedAcl());
		
		storage.setPermission("authenticated-read");
		assertEquals(CannedAccessControlList.AuthenticatedRead, storage.getCannedAcl());
		
		storage.setPermission("log-delivery-write");
		assertEquals(CannedAccessControlList.LogDeliveryWrite, storage.getCannedAcl());
		
		storage.setPermission("bucket-owner-read");
		assertEquals(CannedAccessControlList.BucketOwnerRead, storage.getCannedAcl());
		
		storage.setPermission("bucket-owner-full-control");
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, storage.getCannedAcl());
		
		storage.setPermission("aws-exec-read");
		assertEquals(CannedAccessControlList.AwsExecRead, storage.getCannedAcl());
		
		
	}
}
