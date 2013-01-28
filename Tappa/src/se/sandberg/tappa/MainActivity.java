package se.sandberg.tappa;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

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

		String errorMessage = validateRequestData(date, nrOfSteps, username, password);
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

		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> future = executor.submit(new Callable<String>() {
			public String call() {
				try {
					return registerSteps(date, nrOfSteps, username, password);
				} catch (IOException e) {
					return "Oväntat fel: " + e.getMessage();
				}
			}
		});
		createDialog(future.get(), "Resultat");

	}

	private String validateRequestData(String date, String nrOfSteps,
			String username, String password) {

		if(username == null || username.length() < 1){
			return "Felaktigt användarnamn";
		}

		if(password == null || password.length() < 1){
			return "Felaktigt lösenord";
		}

		if(date == null || date.length() < 1){
			return "Felaktigt datum";
		}

		if(nrOfSteps == null || nrOfSteps.length() < 1){
			return "Felaktigt antal steg";
		}

		try {
			sdf.parse(date);
		} catch (ParseException e) {
			return "Felaktigt datumformat";
		}

		try{
			int parseInt = Integer.parseInt(nrOfSteps);
			if(parseInt < 0){
				return "Felaktigt antal steg";
			}
		}catch (NumberFormatException e) {
			return "Felaktigt antal steg";
		}

		return null;
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

	private String registerSteps(String date, String nrOfSteps, String username, String password) throws IOException {

		String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0";
		String result = "";

		Connection.Response login = Jsoup.connect("http://tappa.se/login/login.ashx")
				.data("jquery", "true", "txtUsername", username, "txtPassword", password) 
				.userAgent(userAgent)
				.method(Method.POST)
				.execute();

		Connection.Response findAccountIdReq = Jsoup.connect("http://www.tappa.se/inside/stepcompetition/steps/")
				.userAgent(userAgent)
				.cookies(login.cookies())
				.method(Method.GET)
				.execute();

		String html = findAccountIdReq.parse().html();
		if(!html.contains("Logga ut")){
			//No login...
			return "Inloggning misslyckades, fel lösenord?";
		}

		String[] split = html.split("accountid=");
		String accountid = split[1].substring(0, split[1].indexOf("&amp"));

		//Register steps
		Connection.Response register = Jsoup.connect("http://www.tappa.se/inside/steps/handlers/StepPost.ashx")
				.data("AccountId", accountid, "SelectedDate", date, "Steps", nrOfSteps) 
				.method(Method.POST)
				.ignoreContentType(true)
				.cookies(login.cookies())
				.execute();

		String body = register.body();
		//Means OK
		if(body != null && body.length() > 0){
			result = "Dina steg har registrerats";
		}

		Jsoup.connect("http://www.tappa.se/inside/Logout.aspx")
		.userAgent(userAgent)
		.cookies(login.cookies())
		.method(Method.GET)
		.execute();

		return result;

	}
}

