package io.antmedia.licence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.rest.model.Result;

public class FirebaseEngine {
	
	protected static Logger logger = LoggerFactory.getLogger(FirebaseEngine.class);
	private static final String DATABASE_URL ="https://ant-media-licence-manager-c4e6d.firebaseio.com/";
	private static DatabaseReference database;

	public Result connectFirebaseDB() {

		Result result = new Result(false);

		 // Initialize Firebase
        try {
            // [START initialize]
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/ant-media-licence-manager-firebase-adminsdk-j9dji-8714e8700c.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            FirebaseApp.initializeApp(options);
            // [END initialize]
        } catch (IOException e) {
            System.out.println("ERROR: invalid service account credentials. See README.");
            System.out.println(e.getMessage());

        }

		return result;
	}

	public Result saveData (Licence licence) {

		Result result = new Result(false);

        // Shared Database reference
        database = FirebaseDatabase.getInstance().getReference();
        
		database.child("alanisawesome").setValueAsync("davut");
		
		logger.info("inside save");


		return result;

	}

}
