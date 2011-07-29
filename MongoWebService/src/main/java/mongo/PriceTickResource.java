package mongo;

import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

@Path("/")
public class PriceTickResource {
	private static final String hostName = "192.168.0.210";
	private static final String dbName = "test";
	private static final String collName = "banana";
	
	// Threshold for strikes above/below current futures price
	private static final double EUD_STRIKE_RANGE = 55;
	private static final double CORN_STRIKE_RANGE = 55;
	private static final double OIL_STRIKE_RANGE = 750;
	private static final double TY_STRIKE_RANGE = 55;
	
	Mongo m;
	
	@GET
	@Produces("text/html")
	public String showWelcome() {
		return "<h1>Welcome to the MongoDB Web Service</h1>";
	}
	
	@GET @Path("/pt/opt/eud")
	@Produces("text/html")
	public String getEUD() throws UnknownHostException {
		
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("ES..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		String result="";
		double futurePrice = -1;
		
		while (cur.hasNext()) {
			BSONObject current = cur.next();
			BasicBSONObject instrument = (BasicBSONObject) current.get("instrument");
			BasicBSONObject latestprices = (BasicBSONObject) current.get("latestprices");
			// If we are processing the future, set the future price
			if (instrument.get("putcall").equals("future") && !latestprices.get("premium").equals(Double.valueOf(0))) {
				futurePrice = Double.parseDouble(latestprices.get("premium").toString());
				result+=current.toString()+"\n";
			}
			// Otherwise, include only options within 100 ticks of the future price
			else {
				Double thisStrike = Double.parseDouble(instrument.get("strike").toString());
				if (thisStrike < futurePrice + EUD_STRIKE_RANGE && thisStrike > futurePrice - EUD_STRIKE_RANGE)
					result+=current.toString()+"\n";
			}
		}
		m.close();
		return result;
	}
	
	@GET @Path("/pt/opt/corn")
	@Produces("text/html")
	public String getCorn() throws UnknownHostException {
		
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("OZC..|ZC..", Pattern.CASE_INSENSITIVE);
				
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);

		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		String result="";
		double futurePrice = -1;
		
		while (cur.hasNext()) {
			BSONObject current = cur.next();
			BasicBSONObject instrument = (BasicBSONObject) current.get("instrument");
			BasicBSONObject latestprices = (BasicBSONObject) current.get("latestprices");
			// If we are processing the future, set the future price
			if (instrument.get("putcall").equals("future") && !latestprices.get("premium").equals(Double.valueOf(0))) {
				futurePrice = Double.parseDouble(latestprices.get("premium").toString());
				result+=current.toString()+"\n";
			}
			// Otherwise, include only options within 100 ticks of the future price
			else {
				Double thisStrike = Double.parseDouble(instrument.get("strike").toString());
				if (thisStrike < futurePrice + CORN_STRIKE_RANGE && thisStrike > futurePrice - CORN_STRIKE_RANGE)
					result+=current.toString()+"\n";
			}
		}
		m.close();
		return result;
	}
	
	@GET @Path("/pt/opt/tenyear")
	@Produces("text/html")
	public String getTenYearNotes() throws UnknownHostException {
		
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("OZN..|ZN..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		String result="";
		double futurePrice = -1;
		
		while (cur.hasNext()) {
			BSONObject current = cur.next();
			BasicBSONObject instrument = (BasicBSONObject) current.get("instrument");
			BasicBSONObject latestprices = (BasicBSONObject) current.get("latestprices");
			// If we are processing the future, set the future price
			if (instrument.get("putcall").equals("future") && !latestprices.get("premium").equals(Double.valueOf(0))) {
				futurePrice = Double.parseDouble(latestprices.get("premium").toString());
				result+=current.toString()+"\n";
			}
			// Otherwise, include only options within 100 ticks of the future price
			else {
				Double thisStrike = Double.parseDouble(instrument.get("strike").toString());
				if (thisStrike < futurePrice + TY_STRIKE_RANGE && thisStrike > futurePrice - TY_STRIKE_RANGE)
					result+=current.toString()+"\n";
			}
		}
		m.close();
		return result;
	}
	
	@GET @Path("/pt/opt/oil")
	@Produces("text/html")
	public String getOil() throws UnknownHostException {
		
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("LO..|CL..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		String result="";
		double futurePrice = -1;
		
		while (cur.hasNext()) {
			BSONObject current = cur.next();
			BasicBSONObject instrument = (BasicBSONObject) current.get("instrument");
			BasicBSONObject latestprices = (BasicBSONObject) current.get("latestprices");
			// If we are processing the future, set the future price
			if (instrument.get("putcall").equals("future") && !latestprices.get("premium").equals(Double.valueOf(0))) {
				futurePrice = Double.parseDouble(latestprices.get("premium").toString());
				result+=current.toString()+"\n";
			}
			// Otherwise, include only options within 100 ticks of the future price
			else {
				Double thisStrike = Double.parseDouble(instrument.get("strike").toString());
				if (thisStrike < futurePrice + OIL_STRIKE_RANGE && thisStrike > futurePrice - OIL_STRIKE_RANGE)
					result+=current.toString()+"\n";
			}
		}
		m.close();
		return result;
	}
	
	public double findEUDPremium() throws UnknownHostException {
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("ES..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		query.append("instrument.putcall", "future");
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		BSONObject obj = (BSONObject) cur.next().get("latestprices");
		m.close();
		
		return Double.parseDouble(obj.get("premium").toString());
		
	}
	
	public double findCornPremium() throws UnknownHostException {
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("ZC..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		query.append("instrument.putcall", "future");
		
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		BSONObject obj = (BSONObject) cur.next().get("latestprices");
		m.close();
		return Double.parseDouble(obj.get("premium").toString());
		
	}
	
	public double findOilPremium() throws UnknownHostException {
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("CL..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		query.append("instrument.putcall", "future");
		
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		BSONObject obj = (BSONObject) cur.next().get("latestprices");
		m.close();
		return Double.parseDouble(obj.get("premium").toString());
		
	}
	
	public double findTenYearPremium() throws UnknownHostException {
		// Create MongoDB connection
		m = new Mongo(hostName);
		
		// Select DB and authenticate
		DB db = m.getDB(dbName);
		char[] passwd = "admin".toCharArray();
		db.authenticate("admin", passwd);
		
		// Find all commodity codes using regex
		Pattern p = Pattern.compile("ZN..", Pattern.CASE_INSENSITIVE);
		
		// Formulate query
		BasicDBObject query = new BasicDBObject("instrument.commodity", p);
		query.append("instrument.putcall", "future");
		
		
		// Get the results and iterate through them
		DBCursor cur = db.getCollection(collName).find(query);
		BSONObject obj = (BSONObject) cur.next().get("latestprices");
		m.close();
		return Double.parseDouble(obj.get("premium").toString());
	}
	
	
}
