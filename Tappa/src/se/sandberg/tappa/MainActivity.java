package se.sandberg.tappa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import android.view.MenuItem;
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
	private RegisterStepsTask registerStepsTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);

		setContentView(R.layout.activity_main);
		((EditText) findViewById(R.id.dateInput)).setText(sdf.format(new Date()));
		((EditText) findViewById(R.id.usernameInput)).setText(preferences.getString("tappaUsername", ""));
		((EditText) findViewById(R.id.passwordInput)).setText(preferences.getString("tappaPassword", ""));
	}

	/**
	 * Called if the activity is stopped due to user changing app, phone call 
	 * or similar action
	 */
	@Override
	protected void onStop() {
		super.onStop();
		if(registerStepsTask != null){
			registerStepsTask.cancel(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(registerStepsTask != null){
			registerStepsTask.cancel(true);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "Stegsnittshämning av");
		menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, "Stegsnittshämning på");	
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		Boolean stepAverage = null;
		if(item.getItemId() == Menu.FIRST){
			stepAverage = Boolean.FALSE;
		}else{
			stepAverage = Boolean.TRUE;
		}
		
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean("tappaStepAverage", stepAverage);
		if(!editor.commit()){
			createDialog("Inställningen kunde inte sparas", "Felmeddelande");
		}
		
		return super.onOptionsItemSelected(item);
	}

	/** 
	 * Called when the user clicks the minus button, it decreases the date
	 * by one day in the date edit text input. 
	 * 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * */
	public void decreaseDate(View view) {
		EditText dateEdit = (EditText) findViewById(R.id.dateInput);
		Date parsedDate = null;
		try {
			parsedDate = sdf.parse(dateEdit.getText().toString());
		} catch (ParseException e) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(parsedDate);
		cal.add(Calendar.DAY_OF_YEAR, -1);
		dateEdit.setText(sdf.format(cal.getTime()));

	}

	/** 
	 * Called when the user clicks the register button 
	 * 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * */
	public void scrape(View view) {

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		
		final String date = ((EditText) findViewById(R.id.dateInput)).getText().toString();
		final String nrOfSteps = ((EditText) findViewById(R.id.nrOfStepsInput)).getText().toString();
		final String username = ((EditText) findViewById(R.id.usernameInput)).getText().toString();
		final String password = ((EditText) findViewById(R.id.passwordInput)).getText().toString();
		final Boolean stepAverage = preferences.getBoolean("tappaStepAverage", Boolean.TRUE);
		
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
			Editor editor = preferences.edit();
			editor.putString("tappaUsername", username);
			editor.putString("tappaPassword", password);
			if(!editor.commit()){
				createDialog("Användarnamnet kunde inte sparas", "Felmeddelande");
			}
		}
		registerStepsTask = new RegisterStepsTask(MainActivity.this, stepAverage);
		registerStepsTask.execute(date, nrOfSteps, username, password);
	}


	private void createDialog(String message, String title){
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(message).setTitle(title);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}