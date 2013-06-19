package se.aptitud.android.imok;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Logger logger = new SimpleLogger(); // DI, of course

	private SimpleLocationListener locationListener;
	private String phoneNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onStart() {
		locationListener = new SimpleLocationListener();

		String provider = getLocationManager().getProvider(LocationManager.NETWORK_PROVIDER).getName();

		getLocationManager().requestLocationUpdates(provider, 0, 0, locationListener);
		this.phoneNumber = getPhoneNumberFromSettings();
		super.onStart();
	}

	private LocationManager getLocationManager() {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		return locationManager;
	}

	@Override
	protected void onStop() {
		stopListeningToLocationUpdates();
		super.onStop();
	}

	private void stopListeningToLocationUpdates() {
		if (locationListener != null) {
			LocationManager locationManager = getLocationManager();
			locationManager.removeUpdates(locationListener);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, 1);
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case 1:
				Uri result = data.getData();
				String contactId = result.getLastPathSegment();

				Cursor cursor = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, null,
						CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { contactId }, null);

				if (cursor.moveToFirst()) {
					this.phoneNumber = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
					savePhoneNumberToSettings(phoneNumber);
					String logMessage = "Saving phone number: " + phoneNumber;
					logger.logInformationMessage(logMessage);
				}
				cursor.close();

				break;
			}
		} else {
			logger.logErrorMessage("Say what?");
		}
	}

	private void savePhoneNumberToSettings(String phoneNumber) {
		SharedPreferences settings = getSharedPreferences("PhoneNumber", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("SelectedPhoneNumber", phoneNumber);
		editor.commit();
	}

	private String getPhoneNumberFromSettings() {
		SharedPreferences settings = getSharedPreferences("PhoneNumber", 0);
		return settings.getString("SelectedPhoneNumber", "");
	}

	public void onImOkBtnClick(View view) {
		sendSmsMessage();
	}

	private void sendSmsMessage() {
		Location location = locationListener.getLocation();
		// String city = getCityForLocation(location);

		SmsManager smsManager = SmsManager.getDefault();
		List<String> smsReceivers = getSmsReceivers();

		for (String smsReceiver : smsReceivers) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			String url = "https://maps.google.com/maps?q=" + latitude + "," + longitude + "&z=7&t=e";
			smsManager.sendTextMessage(smsReceiver, null, "I'm OK (" + url + ")" + "! (TjŠŠŠna)", null, null);
			String logMessage = "Sending I'm OK message to phone number:" + smsReceiver;

			Toast.makeText(this, logMessage, Toast.LENGTH_SHORT).show();
			logger.logInformationMessage(logMessage);
		}
	}

	private String getCityForLocation(Location location) {
		List<Address> list;
		String city = null;
		try {
			list = new Geocoder(this).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (list != null & list.size() > 0) {
				Address address = list.get(0);
				city = address.getLocality();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return city;
	}

	private final class SimpleLocationListener implements LocationListener {

		private static final int MIN_ACCURACY_IN_METER = 2000;

		private Location location;

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onLocationChanged(Location location) {
			float accuracy = location.getAccuracy();

			logger.logInformationMessage("Got location with accuracy " + accuracy);

			if (accuracy <= MIN_ACCURACY_IN_METER) {
				this.location = location;
				stopListeningToLocationUpdates();
			}
		}

		public Location getLocation() {
			return location;
		}

	}

	private List<String> getSmsReceivers() {
		return Arrays.asList(this.phoneNumber);
	}

}
