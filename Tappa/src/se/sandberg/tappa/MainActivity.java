package se.sandberg.tappa;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

/***
 * The MainActivity
 * 
 * @author Andreas Sandberg
 * @date 2013-01-27
 */
public class MainActivity extends Activity {

	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);

		setContentView(R.layout.activity_main);
		((EditText) findViewById(R.id.dateInput)).setText(sdf.format(new Date()));
		((EditText) findViewById(R.id.usernameInput)).setText(preferences.getString("tappaUsername", ""));
		((EditText) findViewById(R.id.passwordInput)).setText(preferences.getString("tappaPassword", ""));


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//No settings menu in this version
		return false;
	}


	/** Called when the user clicks the button 
	 * @throws ExecutionException 
	 * @throws InterruptedException */
	public void scrape(View view) throws InterruptedException, ExecutionException {

		final String date = ((EditText) findViewById(R.id.dateInput)).getText().toString();
		final String nrOfSteps = ((EditText) findViewById(R.id.nrOfStepsInput)).getText().toString();
		final String username = ((EditText) findViewById(R.id.usernameInput)).getText().toString();
		final String password = ((EditText) findViewById(R.id.passwordInput)).getText().toString();

		String errorMessage = Validator.validateRequestData(date, nrOfSteps, username, password);
		if(errorMessage != null){
			createDialog(errorMessage, "Inmatningsfel");
			return;
		}
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			createDialog("Internetanslutning saknas", "Anslutningsfel");
			return;
		}


		if(username != null && password != null){
			savePreferences(username, password);
		}
		RegisterStepsTask registerStepsTask = new RegisterStepsTask(MainActivity.this);
		registerStepsTask.execute(date, nrOfSteps, username, password);
	}
	

	private void savePreferences(final String username, final String password) {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString("tappaUsername", username);
		editor.putString("tappaPassword", password);
		if(!editor.commit()){
			createDialog("Användarnamnet kunde inte sparas", "Felmeddelande");
		}
	}

	private void createDialog(String message, String title){
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(message).setTitle(title);
		AlertDialog dialog = builder.create();
		dialog.show();
	}


}

