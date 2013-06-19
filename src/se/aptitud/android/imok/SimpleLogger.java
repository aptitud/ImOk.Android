package se.aptitud.android.imok;

import android.util.Log;

public class SimpleLogger implements Logger {

	private static final String LOG_TAG = "ImOK";

	@Override
	public void logInformationMessage(String message) {
		Log.i(LOG_TAG, message);
	}

	@Override
	public void logErrorMessage(String message) {
		Log.e(LOG_TAG, message);
	}

}
