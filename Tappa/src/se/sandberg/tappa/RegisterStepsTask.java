package se.sandberg.tappa;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;

/**
 * The RegisterStepTask handles the actual step registration by scraping the tappa.se site.
 * 
 * @author Andreas Sandberg
 */
public class RegisterStepsTask extends AsyncTask<String, String, String> {

	private SharedPreferences preferences;
	private ProgressDialog dialog;
	private final Context context;
	private final Boolean stepAverage;
	private static final int TIMEOUT = 30000;
	private static final Pattern pattern = Pattern.compile("tr class=\"focus\".+?nowrap\">(.+?)</td>.+?</tr>", Pattern.DOTALL);
	private static final String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0";


	public RegisterStepsTask(Context context, Boolean stepAverage, SharedPreferences preferences) {
		super();
		this.context = context;
		this.stepAverage = stepAverage;
		this.preferences = preferences;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		if(dialog != null){
			dialog.dismiss();
		}
	}

	@Override
	protected String doInBackground(final String... params) {

		String date = params[0];
		String nrOfSteps = params[1];
		String username = params[2];
		String password = params[3];
		publishProgress("Loggar in...");
		try {
			return registerSteps(date, nrOfSteps, username, password);
		} catch (IOException e) {
			return "Okänt fel " + notNull(e.getMessage()); 
		}
	}


	private String registerSteps(String date, String nrOfSteps, String username, String password) throws IOException {

		if(isCancelled()){
			saveStatus("Registreringen av " + nrOfSteps + " steg ("+date+") " + "avbröts utan att några steg registrerats");
			return "";
		}
		Connection.Response login = Jsoup.connect("http://tappa.se/login/login.ashx")
				.data("jquery", "true", "txtUsername", username, "txtPassword", password) 
				.userAgent(userAgent)
				.method(Method.POST)
				.timeout(TIMEOUT)
				.execute();


		if(isCancelled()){
			saveStatus("Registreringen av " + nrOfSteps + " steg ("+date+") " + "avbröts utan att några steg registrerats");
			return "";
		}

		Connection.Response findAccountIdReq = Jsoup.connect("http://www.tappa.se/inside/stepcompetition/steps/")
				.userAgent(userAgent)
				.cookies(login.cookies())
				.method(Method.GET)
				.timeout(TIMEOUT)
				.execute();

		if(isCancelled()){
			saveStatus("Registreringen av " + nrOfSteps + " steg ("+date+") " + "avbröts utan att några steg registrerats");
			return "";
		}

		String html = findAccountIdReq.parse().html();
		if(!html.contains("Logga ut")){
			//No login...
			return "Inloggning misslyckades, fel lösenord?";
		}

		String[] split = html.split("accountid=");
		String accountid = split[1].substring(0, split[1].indexOf("&amp"));

		publishProgress("Skickar steg...");
		//Register steps
		Connection.Response register = Jsoup.connect("http://www.tappa.se/inside/steps/handlers/StepPost.ashx")
				.data("AccountId", accountid, "SelectedDate", date, "Steps", nrOfSteps) 
				.method(Method.POST)
				.ignoreContentType(true)
				.cookies(login.cookies())
				.timeout(TIMEOUT)
				.execute();

		String result = "Okänt fel, kontrollera dina registrerade steg.";
		String body = register.body();
		//Means OK
		if(body != null && body.length() > 0){
			result = "Dina steg har registrerats.";
		}

		if(isCancelled()){
			saveStatus("Registreringen av " + nrOfSteps + " steg ("+date+") " + "avbröts och stegen har registrerats");
			return result;
		}

		try{
			if(stepAverage){
				publishProgress("Hämtar stegsnitt...");
				//Try to parse new step average, note that exceptions here may not 
				//cause error messages since the registration has finished.
				Integer stepAverageAfter = retrieveStepAverage(userAgent, login.cookies());
				if(stepAverageAfter != null){
					result += "\nNytt stegsnitt: " + stepAverageAfter;
				}
			}

			if(isCancelled()){
				saveStatus("Registreringen av " + nrOfSteps + " steg ("+date+") " + "avbröts men stegen har registrerats");
				return result;
			}

			//Try to logout - don't parse the results or any errors
			Jsoup.connect("http://www.tappa.se/login/logout.ashx")
			.userAgent(userAgent)
			.cookies(login.cookies())
			.method(Method.GET)
			.execute();
		}catch (Exception e) {
			//Ignore errors here.
		}
		return result;
	}

	private Integer retrieveStepAverage(String userAgent, Map<String, String> cookies) throws IOException {
		Connection.Response findAverage = Jsoup.connect("http://tappa.se/inside/stepcompetition/start/?loggedin=true")
				.userAgent(userAgent)
				.cookies(cookies)
				.method(Method.GET)
				.timeout(TIMEOUT)
				.execute();

		Matcher matcher = pattern.matcher(findAverage.parse().html());
		if(matcher.find()){
			String stepAverage = matcher.group(1).replaceAll(" ", "");
			try{
				return Integer.parseInt(stepAverage);
			}catch(NumberFormatException nfe){
				return null;
			}
		}
		return null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		dialog = new ProgressDialog(context);
		dialog.setMessage("Startar stegregistrering...");
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.show();
	} 

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		if(dialog != null && dialog.isShowing() && !isCancelled()){
			dialog.setMessage(values[0]);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(result).setTitle("Resultat");
		builder.create().show();

	}

	private static String notNull(String s){
		if(s == null){
			return "";
		}
		return s;
	}
	
	private void saveStatus(String statusMessage){
		if(preferences == null){
			return;
		}
		Editor editor = preferences.edit();
		editor.putString("tappaStatusMessage", statusMessage);
		editor.commit();
	}
}
