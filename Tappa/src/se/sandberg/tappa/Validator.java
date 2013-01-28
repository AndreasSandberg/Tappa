package se.sandberg.tappa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;

public class Validator {

	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	public static String validateRequestData(String date, String nrOfSteps, String username, String password) {

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
			Calendar parsedCalendar = Calendar.getInstance();
			parsedCalendar.setTime(sdf.parse(date));
			resetTimeFields(parsedCalendar);
			Calendar now = resetTimeFields(Calendar.getInstance());
			if(parsedCalendar.after(now)){
				return "Du kan bara registrera steg t.o.m dagens datum";
			}
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
	
	private static Calendar resetTimeFields(Calendar cal){
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
}
