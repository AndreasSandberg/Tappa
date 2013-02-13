package se.sandberg.tappa;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * @author Andreas Sandberg
 */
public class RegisterStepsTask extends AsyncTask<String, String, String> {

	private ProgressDialog dialog;
	private final Context context;

	public RegisterStepsTask(Context context) {
		super();
		this.context = context;
	}


	@Override
	protected String doInBackground(final String... params) {

		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> future = executor.submit(new Callable<String>() {
			public String call() {
				try {
					return registerSteps(params);
				} catch (IOException e) {
					return "Oväntat fel: " + notNull(e.getMessage());
				}
			}

			private String registerSteps(String[] params) throws IOException {
				String date = params[0];
				String nrOfSteps = params[1];
				String username = params[2];
				String password = params[3];
				String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0";
				String result = "Okänt fel, dina steg har inte registrerats.";

				publishProgress("Loggar in...");
				Connection.Response login = Jsoup.connect("http://tappa.se/login/login.ashx")
						.data("jquery", "true", "txtUsername", username, "txtPassword", password) 
						.userAgent(userAgent)
						.method(Method.POST)
						.timeout(30000)
						.execute();

				Connection.Response findAccountIdReq = Jsoup.connect("http://www.tappa.se/inside/stepcompetition/steps/")
						.userAgent(userAgent)
						.cookies(login.cookies())
						.method(Method.GET)
						.timeout(30000)
						.execute();
				
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
						.timeout(30000)
						.execute();

				String body = register.body();
				//Means OK
				if(body != null && body.length() > 0){
					result = "Dina steg har registrerats.";
				}
				
				try{
					publishProgress("Hämtar stegsnitt...");
					/*Try to parse new step average, note that exceptions here may not 
					cause error messages since the registration is complete. */
					Connection.Response findAverage = Jsoup.connect("http://tappa.se/inside/stepcompetition/start/?loggedin=true")
							.userAgent(userAgent)
							.cookies(login.cookies())
							.method(Method.GET)
							.timeout(30000)
							.execute();
					
					Pattern pattern = Pattern.compile("tr class=\"focus\".+?nowrap\">(.+?)</td>.+?</tr>", Pattern.DOTALL);
					Matcher matcher = pattern.matcher(findAverage.parse().html());
					if(matcher.find()){
						result += "\nNytt stegsnitt: " + matcher.group(1);
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
		});
		try {
			return future.get();
		} catch (InterruptedException e) {
			return "Oväntat fel: " + notNull(e.getMessage());
		} catch (ExecutionException e) {
			return "Oväntat fel: " + notNull(e.getMessage());
		}
	}
	
	private static String notNull(String s){
		if(s == null){
			return "";
		}
		return s;
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMessage("Startar stegregistrering...");
		dialog.setIndeterminate(true);
		dialog.show();
		super.onPreExecute();
	} 

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		dialog.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if (dialog.isShowing()) {
			dialog.dismiss();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(result).setTitle("Resultat");
		builder.create().show();

	}

}
