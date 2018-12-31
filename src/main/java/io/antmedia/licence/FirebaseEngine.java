package io.antmedia.licence;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import io.antmedia.rest.model.Result;

public class FirebaseEngine {

	private static final String DATABASE_URL ="https://ant-licence.firebaseio.com";

	public Result connectFirebaseDB() {

		Result result = new Result(false);

		// Initialize Firebase
		try (FileInputStream serviceAccount = new FileInputStream("conf/ant-licence-firebase-adminsdk-t4f6f-3e2bc12d03.json")) {
			FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.setDatabaseUrl(DATABASE_URL)
					.build();
			FirebaseApp.initializeApp(options);
			System.out.println("license server connection successfull");
		} catch (IOException e) {
			System.out.println("ERROR: invalid service account credentials.");
			System.out.println(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}



}
