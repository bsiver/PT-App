package com.android.priceticker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;


public class RestClient {
	
	protected final static String mongoHost = "http://192.168.0.210:8080/MongoWebService/pt/";

	public RestClient() {; 
	}
	
	public static String getRequest(String uri) throws Exception {
		BufferedReader in;
		StringBuffer sb = new StringBuffer("Error: Could not connect to MongoDB");
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 5000);
	        HttpGet request = new HttpGet();
	        request.setURI(new URI(mongoHost+uri));
	        HttpResponse response = client.execute(request);
	        in = new BufferedReader
	        (new InputStreamReader(response.getEntity().getContent()));
	        sb = new StringBuffer("");
	        String line = "";
	        String NL = System.getProperty("line.separator");
	        while ((line = in.readLine()) != null) {
	            sb.append(line + NL);
	        }
	        in.close();
		}catch (SocketTimeoutException e) {
			return "Error connecting to MongoDB!";
		}
		catch (Exception e) {
			return "Exception while processing request";
		}
		return sb.toString();
	}
	
}
