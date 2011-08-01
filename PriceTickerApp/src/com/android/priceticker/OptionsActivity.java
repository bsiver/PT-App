package com.android.priceticker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;


/****************************************************
 * Price Ticker App - Options Activity (Main)
 * 
 * @author Ben Siver
 * @date 5/9/2011
 * 
 * This activity represents the initial screen of the Price Ticker App.
 * Users are presented with a drop down menu of four product choices.
 * Upon selecting a product and pressing "Go", a timer is initiated 
 * that calls a web service periodically.  If the web servce is unreachable,
 * the user is prompted to enter a demonstration mode.  The demo mode
 * attempts to read from some pre-recorded data files located on the
 * devices SD card, simulating data streaming in from the web service.
 * If the web service is accessible, the UI is updated at a regular
 * interval with ticker information sent from the web service.  
 * At any time, the user can select to launch the Graphing activity from
 * the menu which sends the data currently being displayed to another
 * activity which then handles plotting.
 *
 *
 *****************************************************
 */
public class OptionsActivity extends Activity implements OnClickListener {
	
	// Global UI elements
	private TableLayout dtLayout;		// Data table page 1
	private TableLayout dtLayout2;		// Data table page 2
	private TextView futureHeader;
	private TextView expireDateHeader;
	private TextView closestStrikeHeader;
	private ToggleButton refreshToggleButton;
	private ViewFlipper dataFlipper;
	private ViewFlipper dataHeadingFlipper;
	
	// Other global variables
	private int rowInsCount;
	private String productChoice;
	private double currentFuturePrice;

	// Google JSON -> Java Mapper
	private Gson gson = new Gson();
	
	// Storage for web service response of PriceTicker objects
	private ArrayList<PriceTick> pt = null;
	
	// AsyncTask used for making calls to web service
	WebServiceTask wst = new WebServiceTask();
	
	// AsyncTask used for managing file IO
	FileAsyncTask fst = new FileAsyncTask();
	
	// Gesture detection
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 120;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
    
    // Constant Strings, error codes, messages, etc.
    private static final String ERROR_NO_ROWS = "Error: It appears the database has no entries\nWould you like to exit?";
    private static final String ERROR_CONNECT = "Connection to database timed out.\nWould you like to exit?";
    private static final String ERROR_NO_PRODUCT = "Please choose a product type before graphing";
    private static final String MONGO_CONNECT_ERR = "Connection to";
    private static final String MONGO_EXCEPTION_ERR = "Exception while processing request";
    private static final String APACHE_CONNECT_ERR = "Apache Tomcat/6.0.28 - Error";
    private static final String RESOLV_ERR = "Connection to the database could not be established\n";
    
    private static final String welcomeMsg = "Please select a product from the drop down menu to begin\n" +
    										 "More information is available in the help activity\n" +
    										 "Press menu to see options and help";
    
    private static final String WEB_SERVICE_IP = "192.168.0.210";
    
    									
    private static final DecimalFormat priceDF = new DecimalFormat("$0");
    private static final DecimalFormat greeksDFLT1 = new DecimalFormat("###.00");
    private static final DecimalFormat greeksDFGT1 = new DecimalFormat(".00000");
    
    
    
    // Colors for TextViews and backgrounds
    private static final int orangeBackground = 0xAFEA8400;
    private static final int blueText = 0xFF1F78B2;
    
    // Time to wait between web service calls
    private static int SLEEP_TIME_MILLIS = 1000;
    
    
    /* Demo File Mode related fields */
    
    // Time to wait between file reads (when in readFromFileMode)
    private static int FILE_SLEEP_TIME_MILLIS = 1000;
    
    // Used for capturing web service data
    private static Boolean fileRecord = false;
    FileWriter f;
    
    private static final String demoPrompt = "Would you like to enter a demo mode which displays previously recorded sample data?";

	/*
	 * onCreate()
	 *
	 * Called when activity is created, does setting up of UI components
	 *
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set custom title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.options);
       
        // Inflate views from XML layout
        dtLayout = (TableLayout) findViewById(R.id.dataTable);
        dtLayout2 = (TableLayout) findViewById(R.id.dataTable2);
        futureHeader = (TextView) findViewById(R.id.displayHeader);
        expireDateHeader = (TextView) findViewById(R.id.expireDateHeader);
        closestStrikeHeader = (TextView) findViewById(R.id.closestStrikeHeader);
        refreshToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        dataFlipper = (ViewFlipper) findViewById(R.id.dataTableFlipper);
        dataHeadingFlipper = (ViewFlipper) findViewById(R.id.ColumnFlipper);
        
        // Set up product choice drop down menu
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.products_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new ProductChoiceSelectionListener());
        
        // Allow Go button to respond to clicks
        refreshToggleButton.setOnClickListener(this);
        
        // Set up gesture detection
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            @Override
			public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        
    	dtLayout.setOnClickListener(OptionsActivity.this); 
        dtLayout.setOnTouchListener(gestureListener);
        dtLayout2.setOnClickListener(OptionsActivity.this); 
        dtLayout2.setOnTouchListener(gestureListener);
        
        // Display welcome message to user
        AlertDialog.Builder welcomeDialog = new AlertDialog.Builder(this);
        welcomeDialog.setMessage(welcomeMsg)
        	.setTitle("Welcome!")
        	.setIcon(R.drawable.ic_menu_globe)
        	.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 dialog.cancel();
            }
        });
        AlertDialog welcomeMessage = welcomeDialog.create();
        welcomeMessage.show();
     }
    
    /*
     * class MyGestureDetector
     *  
     * This class is used to respond to user gestures on the data table layout
     * Swiping to the left or right when data is present causes the ViewFlipper
     * to change its current display to the next page.
     * 
     * Referenced code for gesture detection can be found here:
     * http://savagelook.com/blog/android/swipes-or-flings-for-navigation-in-android
     * 
     */
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	dataFlipper.showNext();
                	dataHeadingFlipper.showNext();
                	Toast.makeText(OptionsActivity.this, "Page 2", Toast.LENGTH_SHORT);
                }  
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    dataFlipper.showNext();
                    dataHeadingFlipper.showNext();
                    Toast.makeText(OptionsActivity.this, "Page 1", Toast.LENGTH_SHORT);
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

    /*
     * onCreateOptionsMenu()
     * 
	 * Called when user presses hardware menu button
	 * Displays options to toggle between activities
	 *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }

    /*
     * onOptionsItemSelected()
     * 
     * Called when user makes a choice in activity selection view
     * Handles switching of activities by sending intents
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.graphingIcon:
            	if (pt != null) {
	            	Intent myIntent = new Intent(OptionsActivity.this, GraphingActivity.class);
	            	myIntent.putExtra("pt", generateGraphParcel());
	            	myIntent.putExtra("futuresPrice", currentFuturePrice);
	            	OptionsActivity.this.startActivity(myIntent);
            	}
            	else Toast.makeText(this, ERROR_NO_PRODUCT, Toast.LENGTH_SHORT).show();
                break;
            case R.id.helpIcon: 
            	Intent myIntent = new Intent(OptionsActivity.this, HelpActivity.class);
            	OptionsActivity.this.startActivity(myIntent);
                break;
            case R.id.exitIcon:
            	System.exit(0);
        }
        return true;
    }

	/*
     * MyOnItemSelectedListener
     * 
     * Listener for the spinner menu
     * 
     * Handles the respective web service call by creating an AsyncTask - see WebServiceTask for more info.
     * After retrieving array of PriceTick objects, this method populates a table layout with the data received
     * from the web service call.
     */
    public class ProductChoiceSelectionListener implements OnItemSelectedListener {

        @Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            // Get & set the user's product choice
        	String choice = parent.getItemAtPosition(pos).toString();
        	productChoice = choice;
        	
        	// Kill & renew AsyncTasks
        	wst.cancel(false);
        	wst = new WebServiceTask();
        	fst.cancel(false);
        	fst = new FileAsyncTask();
        	
        	// Clear data views
        	futureHeader.setText("");
        	expireDateHeader.setText("");
        	closestStrikeHeader.setText("");
        	dtLayout.removeAllViews();
        	dtLayout2.removeAllViews();
        	
        	// Start refreshing new product choice
        	if (refreshToggleButton.isChecked()) {
        		wst.execute(productChoice);
        	}
        	
        	
        }

        @Override
		public void onNothingSelected(AdapterView<?> parent) {
          // Do nothing.
        }
    }
    
    /*
     * onClick
     * 
     * Listener for the clickable elements in the UI
     *       
     * Executes the web service task with the current product choice
     *   
     *
     */
    @Override
	public void onClick(View v) {
    	
    	if (refreshToggleButton.isChecked() && v.equals(refreshToggleButton)) {
			
	    	try {
	    		// Check if web service is reachable
	    		InetAddress in = InetAddress.getByName(WEB_SERVICE_IP);
	    		if (in.isReachable(2000)) {
	    			wst.execute(productChoice);
	    		}
	    		else {
	    			// Prompt user to enter demo mode
	    			demoPrompt("Connection to the database could not be established");
	    		}
	    		
	    	} catch (Exception e) {}
    	}
    	else if (!refreshToggleButton.isChecked() && v.equals(refreshToggleButton)) {
    		fst.cancel(false);
    		fst = new FileAsyncTask();
    		wst.cancel(false);
    		wst = new WebServiceTask();
    	}
    	
    }

   

	/*
     * populateRowsLandscape()
     * 
     * Populates the data table when phone is in landscape orientation
     * Note that landscape view uses a ViewFlipper to show even
     * more data.
     * 
     * The respective columns in landscape mode tab 1 are:
     * 		Call Bid
     *		Call Ask
     *		Strike
     *		Put Bid
     *      Put Ask
     *      Δ Delta
     *      Γ Gamma 
     *		ν Vega
     *     
     * The respective columns in landscape mode tab 2 are:
     * 		Call Bid
     *		Call Ask
     *		Strike
     *		Put Bid
     *      Put Ask
     *      Θ Theta
     *      ρ Rho
     *
     */
    public void populateRowsLandscape(ArrayList<PriceTick> pt) {	
    	
    	if (pt.size() == 1) return;
    	rowInsCount = 0;
    	
    	// Clear previous table data
    	dtLayout.removeAllViews();
    	dtLayout2.removeAllViews();
    	futureHeader.setText("");
    	
    	String expireDate = "";
    	double closestStrike = Double.MAX_VALUE;
    	
        for (PriceTick p: pt) {
        	
        	// If we are processing the future, set the current future price and then skip this row
        	if (p.getInstrument().getPutcall().equals("future") && p.getLatestprices().getPremium() != 0) {
        		currentFuturePrice = p.getLatestprices().getPremium();
        		futureHeader.setText("Current Future Price: " +priceDF.format(currentFuturePrice));
        		expireDate = p.getInstrument().getExpiredate();
        		expireDateHeader.setText("Expire date: "+expireDate);
        		continue;
        	}
        	
        	// If this is the first insert, look through the array to find the closest strike
        	if (rowInsCount == 0)
        		closestStrike = findClosestStrike();
		
	        // Determine the text of the row
	        String strike="";
	        String bidPrice="";
	        String askPrice="";
	        String putCall="";
	        String delta="";
	        String gamma="";
	        String vega = "";
	        String theta="";
	        String rho="";
	        
	        strike = priceDF.format(p.getInstrument().getStrike());
	        bidPrice = priceDF.format(p.getLatestprices().getBidprice());
	        askPrice = priceDF.format(p.getLatestprices().getAskprice());
	        putCall = p.getInstrument().getPutcall();
        	delta = greeksDFGT1.format(p.getGreeks().getDelta());
        	gamma = greeksDFGT1.format(p.getGreeks().getGamma());
	        theta = greeksDFLT1.format(p.getGreeks().getTheta());
	        vega = greeksDFLT1.format(p.getGreeks().getVega());
	        rho = greeksDFLT1.format(p.getGreeks().getRho());
	        
	        // Start building row
	    	TableRow tr = new TableRow(this);
	    	TableRow tr2 = new TableRow(this);
	    
	        // Build TextViews
	        TextView callBidTV, callAskTV, strikeTV, putBidTV, putAskTV, 
	        		 strikeTV2, deltaTV, gammaTV, vegaTV, thetaTV, rhoTV,
	        		 callBidTV2, callAskTV2, putBidTV2, putAskTV2;
			callBidTV = new TextView(this);
			callAskTV = new TextView(this);
			strikeTV = new TextView(this);
			putBidTV = new TextView(this);
			putAskTV = new TextView(this);
			strikeTV2 = new TextView(this);
			deltaTV = new TextView(this);
			gammaTV = new TextView(this);
			vegaTV = new TextView(this);
			thetaTV = new TextView(this);
			rhoTV = new TextView(this);
			callBidTV2 = new TextView(this);
			callAskTV2 = new TextView(this);
			putBidTV2 = new TextView(this);
			putAskTV2 = new TextView(this);

			
			callBidTV.setTextColor(blueText);
			callAskTV.setTextColor(blueText);
			strikeTV.setTextColor(blueText);
			putBidTV.setTextColor(blueText);
			putAskTV.setTextColor(blueText);
			strikeTV2.setTextColor(blueText);
			deltaTV.setTextColor(blueText);
			gammaTV.setTextColor(blueText);
			vegaTV.setTextColor(blueText);
			thetaTV.setTextColor(blueText);
			rhoTV.setTextColor(blueText);
			callBidTV2.setTextColor(blueText);
			callAskTV2.setTextColor(blueText);
			putBidTV2.setTextColor(blueText);
			putAskTV2.setTextColor(blueText);
			
			callBidTV.setTextSize(18);
			callAskTV.setTextSize(18);
			strikeTV.setTextSize(18);
			putBidTV.setTextSize(18);
			putAskTV.setTextSize(18);
			strikeTV2.setTextSize(18);
			deltaTV.setTextSize(18);
			gammaTV.setTextSize(18);
			vegaTV.setTextSize(18);
			thetaTV.setTextSize(18);
			rhoTV.setTextSize(18);
			callBidTV2.setTextSize(18);
			callAskTV2.setTextSize(18);
			putBidTV2.setTextSize(18);
			putAskTV2.setTextSize(18);

		    callBidTV.setGravity(Gravity.RIGHT);
		    callAskTV.setGravity(Gravity.RIGHT);
			strikeTV.setGravity(Gravity.RIGHT);
			putBidTV.setGravity(Gravity.RIGHT);
			putAskTV.setGravity(Gravity.RIGHT);
			strikeTV2.setGravity(Gravity.RIGHT);
			deltaTV.setGravity(Gravity.RIGHT);
			gammaTV.setGravity(Gravity.RIGHT);
			vegaTV.setGravity(Gravity.RIGHT);
			thetaTV.setGravity(Gravity.RIGHT);
			rhoTV.setGravity(Gravity.RIGHT);
			callBidTV2.setGravity(Gravity.RIGHT);
			callAskTV2.setGravity(Gravity.RIGHT);
			putBidTV2.setGravity(Gravity.RIGHT);
			putAskTV2.setGravity(Gravity.RIGHT);

			
			if (putCall.equals("C")) {
				callBidTV.setText(bidPrice);
				callAskTV.setText(askPrice);
				strikeTV.setText(strike);
				double [] bidAsk = findMatchingBidAsk(p.getInstrument().getStrike(), "P");
				putBidTV.setText(priceDF.format(bidAsk[0]));
				putAskTV.setText(priceDF.format(bidAsk[1]));
				putBidTV2.setText(priceDF.format(bidAsk[0]));
				putAskTV2.setText(priceDF.format(bidAsk[1]));
				strikeTV2.setText(strike);
				deltaTV.setText(delta);
				gammaTV.setText(gamma);
				vegaTV.setText(vega);
				thetaTV.setText(rho);
				rhoTV.setText(theta);
				callBidTV2.setText(bidPrice);
				callAskTV2.setText(askPrice);

			}
			
			double thisStrike = p.getInstrument().getStrike();
			// If current strike is less than the current future price, color it green
			if (thisStrike < currentFuturePrice) {
				strikeTV.setTextColor(Color.GREEN);
				strikeTV2.setTextColor(Color.GREEN);
			}
			
			// If current strike is greater than the current future price, color it red			
			if (thisStrike > currentFuturePrice) {
				strikeTV.setTextColor(Color.RED);
				strikeTV2.setTextColor(Color.RED);
			}
			
			// If this strike is the closest strike, set the background color to gray and display the current future price
			if (thisStrike == closestStrike) {
				tr.setBackgroundColor(orangeBackground);
				tr2.setBackgroundColor(orangeBackground);
			}

			tr.addView(callBidTV);
			tr.addView(callAskTV);
			tr.addView(strikeTV);
			tr.addView(putBidTV);
			tr.addView(putAskTV);
			tr.addView(deltaTV);
			tr.addView(gammaTV);
			tr.addView(vegaTV);
			
			tr2.addView(callBidTV2);
			tr2.addView(callAskTV2);
			tr2.addView(strikeTV2);
			tr2.addView(putBidTV2);
			tr2.addView(putAskTV2);
			tr2.addView(thetaTV);
			tr2.addView(rhoTV);
			tr2.addView(new TextView(this));
		
			// Following two views create a narrow gray line to act as a table row separator
			View v = new View(this);
			v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, 1));
			v.setBackgroundColor(Color.argb(255, 233, 233, 234));
			
			View v2 = new View(this);
			v2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, 1));
			v2.setBackgroundColor(Color.argb(255, 233, 233, 234));
			
			// Add TableRows to table
			dtLayout.addView(tr);
			dtLayout.addView(v);
			dtLayout2.addView(tr2);
			dtLayout2.addView(v2);
			
			rowInsCount++;

        }
        closestStrikeHeader.setText("Closest strike: "+priceDF.format(closestStrike));
    }
    
    /*
     * findMatchingBidAsk(double strike, String putcall)
     * 
     * This is a helper method used when populating the data table display
     * Given a particular strike, this method will return an array consisting
     * of two doubles: the bid and ask price corresponding to that strike.  The
     * second parameter indicates whether you are looking for the put or call option.
     * 
     * Results are returned as [bidPrice, askPrice]
     * 
     */
    private double [] findMatchingBidAsk(double strike, String putCall) {
    	double [] result = new double[2];
		for (PriceTick p: pt) {
			if (putCall.equals("C")) {
				if (p.getInstrument().getStrike() == strike && p.getInstrument().getPutcall().equals("C")) {
					result[0] = p.getLatestprices().getBidprice();
					result[1] = p.getLatestprices().getAskprice();
				}
			}
			if (putCall.equals("P")) {
				if (p.getInstrument().getStrike() == strike && p.getInstrument().getPutcall().equals("P")) {
					result[0] = p.getLatestprices().getBidprice();
					result[1] = p.getLatestprices().getAskprice();
				}
			}
			
		}
		return result;
	}
    
    /*
     * findClosestStrike()
     * 
     * Iterates through list of PriceTick objects and returns the numeric
     * value of the Strike closest to the current future price
     * 
     */
    public double findClosestStrike() {
    	double lowestStrikeDiff = Double.MAX_VALUE;
    	double closestStrike = Double.MAX_VALUE;
    	
		for (PriceTick p: pt) {
			 // Determine if this strike is the closest to the current future price
		    double thisStrike = p.getInstrument().getStrike();
			if (Math.abs(thisStrike - currentFuturePrice) < lowestStrikeDiff) {
				lowestStrikeDiff = Math.abs(thisStrike - currentFuturePrice);
				closestStrike = p.getInstrument().getStrike();
			}
		}
		return closestStrike;
	}
    

    private void showError(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg)
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           @Override
				public void onClick(DialogInterface dialog, int id) {
		        	   OptionsActivity.this.finish();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           @Override
				public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
		wst.cancel(true);
    }
    
    public void demoPrompt(String msg) {
    	AlertDialog.Builder welcomeDialog = new AlertDialog.Builder(this);
        welcomeDialog.setMessage(msg+"\n"+demoPrompt)
        	.setTitle("Demo Mode")
        	.setIcon(R.drawable.ic_warning)
        	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	fst.execute(productChoice);
            }
            })
        	.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 dialog.cancel();
	    		 if (refreshToggleButton.isChecked())
	    			 refreshToggleButton.toggle();
            }
        });
        AlertDialog welcomeMessage = welcomeDialog.create();
        welcomeMessage.show();
    }
    
    public void setPTArray(ArrayList<PriceTick> pt) {
		this.pt = pt;
	}
    
    public ArrayList<PriceTick> getPTArray() {
		return pt;
	}

    
    /*
     * class WebServiceTask
     * 
     * This class provides an implementation of AsyncTask that continuously polls a
     * MongoDB server for new data.  The sleep interval is set as the static
     * constant SLEEP_TIME_MILLIS.  The AsyncTask runs in an infinite loop until it is
     * canceled by the user turning off auto refresh or when a new product type is selected.
     * 
     * This class also handles the parsing of the service's JSON response to a Java object
     * using the google-gson driver.
     * 
     * The actual HTTP call is externalized to the RestClient class.
     * 
     */
	public class WebServiceTask extends AsyncTask<String, ArrayList<PriceTick>, String> {
    	private ArrayList <PriceTick> arr;
    	
    	@Override
    	protected String doInBackground(String... request) {
    		while (true) {
	    		String response="";
	            try { 
	            	response =  getResponse(request[0]);
	            } catch (Exception e) {
	            	return RESOLV_ERR;
	            }
	            if (response.equals(MONGO_CONNECT_ERR))
	            	return RESOLV_ERR;
	            // If the connection could not be made, display an error
	        	if (response==null || response.equals(MONGO_CONNECT_ERR) || 
	        		response.equals(MONGO_EXCEPTION_ERR) || response.contains(APACHE_CONNECT_ERR)) {
	        		return RESOLV_ERR;
	        	}
	        	// Otherwise, parse the result and populate the data table
	            arr = parseJSON(response);
	            sortArray();
	            publishProgress(arr);
	            try { Thread.sleep(SLEEP_TIME_MILLIS); } catch(Exception e) {}
    		}
        }
    	
    	protected void onPostExecute(String error) {
    		demoPrompt(error);
    	}
    	
    	protected void onProgressUpdate(ArrayList<PriceTick>... result) {
    		OptionsActivity act = OptionsActivity.this;
            act.setPTArray(arr);
            // Check to make sure the parsed array actually contains PT objects
            // If not, show an error and cancel further execution of AsyncTask
            if (pt.get(0) instanceof PriceTick) {
	            	act.populateRowsLandscape(arr);
            }
            else {
            	showError(ERROR_NO_ROWS);
            }
    	}

    	public String getResponse(String request) throws IOException {
        	String webPathRequest = ""; 
        	String response="";
        	if (request.equals("EUD")) webPathRequest = "opt/eud";
        	if (request.equals("Corn")) webPathRequest = "opt/corn";
        	if (request.equals("Oil")) webPathRequest = "opt/oil";
        	if (request.equals("Ten Year Notes")) webPathRequest = "opt/tenyear";
        	try {response = RestClient.getRequest(webPathRequest);}
        		catch (SocketTimeoutException e) {
        			return MONGO_CONNECT_ERR;
        		}
        		catch (Exception e) { 
        			return MONGO_CONNECT_ERR;
        		}
            return response;
        }
    	
        protected ArrayList<PriceTick> parseJSON(String response) {
        	if (fileRecord) {
	        	try { 
	        		f = new FileWriter("/mnt/sdcard/download/"+productChoice+".txt", true);
	        		f.write(response);
	        		f.write("EOL\n");
	        		f.close();
	        	} catch (Exception e) { }
        	}
        	String [] split = response.split("\n");
        	ArrayList<PriceTick> arr = new ArrayList<PriceTick>();
        	JsonParser p = new JsonParser();
        	for (int ind = 0; ind < split.length; ind++) {
        		PriceTick pt = gson.fromJson(p.parse(split[ind]), PriceTick.class); 
        		arr.add(pt);
        	}
        	return arr;
        }
        
        protected void sortArray() {
        	// Sort based on strike (descending)
    	    Collections.sort(arr);
    	    Collections.reverse(arr);
        }

    }
	

	public class FileAsyncTask extends AsyncTask<String, ArrayList<PriceTick>, String> {
    	private ArrayList <PriceTick> arr;
    	File sdcard = Environment.getExternalStorageDirectory();
    	File file = new File(sdcard+"/Download/",productChoice+".txt");

    	StringBuilder text = new StringBuilder();
    	int linesRead = 0;

    	@Override
    	protected String doInBackground(String... request) {
    		while (true) {
	    		String response="";
	            try { 
	            	response =  getResponse(request[0]);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            }
	            if (response.equals(MONGO_CONNECT_ERR))
	            	return null;
	            // If the connection could not be made, display an error
	        	if (response==null || response.equals(MONGO_CONNECT_ERR) || 
	        		response.equals(MONGO_EXCEPTION_ERR) || response.contains(APACHE_CONNECT_ERR)) {
	        		showError(ERROR_CONNECT);
	        	}
	        	// Otherwise, parse the result and populate the data table
	            arr = parseJSON(response);
	            sortArray();
	            publishProgress(arr);
	            try { Thread.sleep(FILE_SLEEP_TIME_MILLIS); } catch(Exception e) {}
    		}
        }
    	
    	protected void onProgressUpdate(ArrayList<PriceTick>... result) {
    		OptionsActivity act = OptionsActivity.this;
            act.setPTArray(arr);
            // Check to make sure the parsed array actually contains PT objects
            // If not, show an error and cancel further execution of AsyncTask
            if (pt.get(0) instanceof PriceTick) {
	            	act.populateRowsLandscape(arr);
            }
    	}

    	public String getResponse(String request) throws IOException {
	    	File sdcard = Environment.getExternalStorageDirectory();
	    	
	    	File file = new File(sdcard+"/Download/",productChoice+".txt");

	    	StringBuilder text = new StringBuilder();

	    	try {
	    	    Scanner sc = new Scanner(file);
	    	    for (int ind = 0; ind < linesRead; sc.nextLine(), ind++);
	    	    while (true) {
	    	    	String line="";
	    	    	if (sc.hasNextLine())
	    	    		line = sc.nextLine();
	    	    	else {
	    	    		fst.cancel(false);
	    	    		fst = new FileAsyncTask();
	    	    	}
	    	    	linesRead++;
	    	    	if (line.equals("EOL")) break;
	    	        text.append(line);
	    	        text.append("\n");
	    	        
	    	    }
	    	}
	    	catch (IOException e) {
	    	}
	    	
            return text.toString();
        }
    	
        protected ArrayList<PriceTick> parseJSON(String response) {
        	String [] split = response.split("\n");
        	ArrayList<PriceTick> arr = new ArrayList<PriceTick>();
        	JsonParser p = new JsonParser();
        	for (int ind = 0; ind < split.length; ind++) {
        		PriceTick pt = gson.fromJson(p.parse(split[ind]), PriceTick.class);
        		arr.add(pt);
        	}
        	return arr;
        }
        
        protected void sortArray() {
        	// Sort based on strike (descending)
    	    Collections.sort(arr);
    	    Collections.reverse(arr);
        }

        private void showError(String msg) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(OptionsActivity.this);
    		builder.setMessage(msg)
    		       .setCancelable(false)
    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    		           @Override
					public void onClick(DialogInterface dialog, int id) {
    		                OptionsActivity.this.finish();
    		           }
    		       })
    		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    		           @Override
					public void onClick(DialogInterface dialog, int id) {
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
    		fst.cancel(true);
        }
    }

	/*
	 * generateGraphParcel()
	 * 
	 * This method captures the current data display with the intent of
	 * sending all data over to the graphing activity.  The data structure used
	 * to store this parcel is a HashMap with a string identifier as the key,
	 * and the value being a list of Doubles which represent the actual numeric
	 * values.
	 * 
	 */
    private HashMap<String, ArrayList<Double>> generateGraphParcel() {
    	HashMap<String, ArrayList<Double>> result = new HashMap<String, ArrayList<Double>>();
    	
    	// Storage for all info associated with a put
    	ArrayList<Double> putDeltas = new ArrayList<Double>();
    	ArrayList<Double> putGammas = new ArrayList<Double>();
    	ArrayList<Double> putVegas = new ArrayList<Double>();
    	ArrayList<Double> putThetas = new ArrayList<Double>();
    	ArrayList<Double> putRhos = new ArrayList<Double>();
    	ArrayList<Double> putStrikes = new ArrayList<Double>();
    	ArrayList<Double> putBids = new ArrayList<Double>();
    	ArrayList<Double> putAsks = new ArrayList<Double>();
    	
    	// Storage for all info associated with a call
    	ArrayList<Double> callDeltas = new ArrayList<Double>();
    	ArrayList<Double> callGammas = new ArrayList<Double>();
    	ArrayList<Double> callVegas = new ArrayList<Double>();
    	ArrayList<Double> callThetas = new ArrayList<Double>();
    	ArrayList<Double> callRhos = new ArrayList<Double>();
    	ArrayList<Double> callStrikes = new ArrayList<Double>();
    	ArrayList<Double> callBids = new ArrayList<Double>();
    	ArrayList<Double> callAsks = new ArrayList<Double>();
    	
		for (PriceTick p: pt) {
			if (p.getInstrument().getPutcall().equals("P")) {
				putDeltas.add(p.getGreeks().getDelta());
				putGammas.add(p.getGreeks().getGamma());
				putVegas.add(p.getGreeks().getVega());
				putThetas.add(p.getGreeks().getTheta());
				putRhos.add(p.getGreeks().getRho());
				putStrikes.add(p.getInstrument().getStrike());
				putBids.add(p.getLatestprices().getBidprice());
				putAsks.add(p.getLatestprices().getAskprice());
			}
			if (p.getInstrument().getPutcall().equals("C")) {
				callDeltas.add(p.getGreeks().getDelta());
				callGammas.add(p.getGreeks().getGamma());
				callVegas.add(p.getGreeks().getVega());
				callThetas.add(p.getGreeks().getTheta());
				callRhos.add(p.getGreeks().getRho());
				callStrikes.add(p.getInstrument().getStrike());
				callBids.add(p.getLatestprices().getBidprice());
				callAsks.add(p.getLatestprices().getAskprice());
			}
			
		}
		result.put("deltaPut", putDeltas);
		result.put("gammaPut", putGammas);
		result.put("vegaPut", putVegas);
		result.put("thetaPut", putThetas);
		result.put("rhoPut", putRhos);
		result.put("strikePut", putStrikes);
		result.put("bidPut", putBids);
		result.put("askPut", putAsks);
		
		result.put("deltaCall", callDeltas);
		result.put("gammaCall", callGammas);
		result.put("vegaCall", callVegas);
		result.put("thetaCall", callThetas);
		result.put("rhoCall", callRhos);
		result.put("strikeCall", callStrikes);
		result.put("bidCall", callBids);
		result.put("askCall", callAsks);
		
		return result;
	}

    
}