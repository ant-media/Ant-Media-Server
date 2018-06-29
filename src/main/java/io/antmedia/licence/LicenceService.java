package io.antmedia.licence;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.red5.logging.Red5LoggerFactory;
import org.red5.spring.Red5ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.gson.Gson;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;

public class LicenceService {
	private static DatabaseReference ref;
	private static DatabaseReference usersRef;
	private ServerSettings serverSettings;
	private FirebaseEngine engine = new FirebaseEngine();
	private boolean responseReceived = false;
	public static final String FIREBASE_CHILD = "licences"; 
	@Context
	private ServletContext servletContext;
	private String activeLicence = "processing";
	private String oldKey = null;
	private Result licenceStatusResponse = new Result(false);
	Gson gson = new Gson();



	public void start() {
		engine.connectFirebaseDB();

		System.out.println("LicenceService has started");
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				System.out.println( " licence key in the file: " + fetchServerSettings().getLicenceKey());
				checkLicence(fetchServerSettings().getLicenceKey());
			}
		}, 3000, 5000);
	}


	public Result saveLicence (Licence licence) {

		Result result = new Result(false);

		ref = FirebaseDatabase.getInstance().getReference();

		usersRef = ref.child(FIREBASE_CHILD);

		usersRef.child(licence.getLicenceId()).setValueAsync(licence);
		result.setSuccess(true);

		return result;
	}

	public String getLicence (String key) {

		ref = FirebaseDatabase.getInstance().getReference();

		DatabaseReference licenceRef = ref.child(FIREBASE_CHILD);
		Query queryRef = licenceRef.orderByKey().equalTo(key);

		queryRef.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot snapshot, String previousChild) {
				System.out.println( "value :  " + snapshot.getValue().toString());
				setResponseReceived(true);
				setActiveLicence(snapshot.getValue().toString());

				oldKey = key;
			}

			@Override
			public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
			}

			@Override
			public void onChildRemoved(DataSnapshot snapshot) {

			}

			@Override
			public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
			}

			@Override
			public void onCancelled(DatabaseError error) {

			}
		});

		/*
		licenceRef.child(key).addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot snapshot) {


				if (snapshot.getChildren() != null) {
					System.out.println( "value " + snapshot.getKey());
					setActiveLicence(snapshot.getValue().toString());
					setResponseReceived(true);
				}

			}
			@Override
			public void onCancelled(DatabaseError error) {
			}
		});

		 */
		return getActiveLicence();
	}


	public Result checkLicence (String key) {
		
		Result result = new Result(false);

		if(!key.equals(oldKey)) {
			setResponseReceived(false);
		}

		getLicence(key);

		if(getResponseReceived()) {
			System.out.println( " licence found: " + getActiveLicence());
			result.setSuccess(true);

			Licence licence = gson.fromJson( getActiveLicence(), Licence.class);
			System.out.println("end date " +licence.getEndDate());

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

			try {
				Date licenceEndDate = dateFormat.parse(licence.getEndDate());

				Date date=new Date();
				String formattedDate=dateFormat.format(date);

				System.out.println("current date " +formattedDate);

				Date currentDate;

				currentDate = dateFormat.parse(formattedDate);

				if(licenceEndDate.after(currentDate) || licenceEndDate.equals(currentDate)) {

					getLicenceStatusResponse().setSuccess(true);
					getLicenceStatusResponse().setMessage(getActiveLicence());

					System.out.println( " licence date is after current ");

				}else {
					getLicenceStatusResponse().setSuccess(false);
					getLicenceStatusResponse().setMessage("license is found but expired");
					System.out.println( "license is found but expired");

				}

			} catch (ParseException e) {
				e.printStackTrace();
			}

		}

		else {
			System.out.println( " no licence found: ");
			result.setMessage("no licence found");
			getLicenceStatusResponse().setSuccess(false);
			
			getLicenceStatusResponse().setMessage("no licence found");
		}

		return result;
	}

	public ServerSettings getServerSettings() {
		return serverSettings;
	}

	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}

	public boolean getResponseReceived() {
		return responseReceived;
	}

	public void setResponseReceived(boolean responseReceived) {
		this.responseReceived = responseReceived;
	}

	public String getActiveLicence() {
		return activeLicence;
	}

	public void setActiveLicence(String res) {
		this.activeLicence = res;
	}

	public ServerSettings fetchServerSettings() {
		if (serverSettings == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			serverSettings = (ServerSettings)ctxt.getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}


	public Result getLicenceStatusResponse() {
		return licenceStatusResponse;
	}


	public void setLicenceStatusResponse(Result licenceStatusResponse) {
		this.licenceStatusResponse = licenceStatusResponse;
	}




}
