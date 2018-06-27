package io.antmedia.licence;

import java.util.Timer;
import java.util.TimerTask;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;

public class LicenceService {
	private static DatabaseReference ref;
	private static DatabaseReference usersRef;
	private ServerSettings serverSettings;
	
	public Result saveLicence (Licence licence) {

		Result result = new Result(false);

		ref = FirebaseDatabase.getInstance().getReference();

		usersRef = ref.child("licences");
		
		usersRef.child(licence.getLicenceId()).setValueAsync(licence);


		return result;

	}
	
	public Licence getLicence (Licence userLicence) {

		Licence licence = new Licence();
		
		ref = FirebaseDatabase.getInstance().getReference();

		usersRef = ref.child("licences");

		return licence;

	}
	
	public void start() {
		System.out.println("-------------------------------------");
		System.out.println("LicenceService.start()");
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				System.out.println("Server settings: " + serverSettings + " licence key: " + serverSettings.getLicenceKey());
				
			}
		}, 5000, 5000);
	}
	
	public Result checkLicence (String key) {

		Result result = new Result(false);

		ref = FirebaseDatabase.getInstance().getReference();

		usersRef = ref.child("licences");
		

		return result;

	}

	public ServerSettings getServerSettings() {
		return serverSettings;
	}

	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}
	


}
