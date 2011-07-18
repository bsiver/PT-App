package com.android.priceticker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

/****************************************************
 * Price Ticker App - Graphing Activity
 * 
 * @author Ben Siver
 * @date 6/28/2011
 * 
 * This activity is launched from the OptionsActivity when a user selects the
 * Graphing choice from the context menu.  Various data from the OptionsActivity's
 * PriceTick array is bundled into a HashMap and sent over to the GraphingActivity.
 * This activity unpackages this data and creates numerous ArrayLists containing
 * the raw numerical data.  AndroidPlot is used to create series plots as well as
 * other graphing features.
 *
 *
 *****************************************************
 */

public class GraphingActivity extends Activity {
	private XYPlot mySimpleXYPlot;
	private Random random;
	 
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphing);
                
        // Retrieve and unpackage graphing data sent from Options activity
        HashMap<String, ArrayList<Double>> serializableExtra = (HashMap<String, ArrayList<Double>>) getIntent().getSerializableExtra("pt");
		HashMap<String, ArrayList<Double>> hm = serializableExtra;
        ArrayList<Double> putBidSeries = hm.get("bidPut");
        ArrayList<Double> putAskSeries = hm.get("askPut");
        ArrayList<Double> putStrikeSeries = hm.get("strikePut");
        
        ArrayList<Double> callBidSeries = hm.get("bidCall");
        ArrayList<Double> callAskSeries = hm.get("askCall");
        ArrayList<Double> callStrikeSeries = hm.get("strikeCall");
        
        // Initialize our XYPlot reference:
        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        
 
        // Turn the above ArrayLists into XYSeries:
       XYSeries callBidVsStrike = new SimpleXYSeries(
                callStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
                callBidSeries, // Y_VALS_ONLY means use the element index as the x value
                "Call Bid");                             // Set the display title of the series
       
       XYSeries callAskVsStrike = new SimpleXYSeries(
               callStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               callAskSeries, // Y_VALS_ONLY means use the element index as the x value
               "Call Ask");                             // Set the display title of the series
       
       XYSeries putAskVsStrike = new SimpleXYSeries(
               putStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               putAskSeries, // Y_VALS_ONLY means use the element index as the x value
               "Put Ask");                             // Set the display title of the series
       
       XYSeries putBidVsStrike = new SimpleXYSeries(
               putStrikeSeries,          // SimpleXYSeries takes a List so turn our array into a List
               putBidSeries, // Y_VALS_ONLY means use the element index as the x value
               "Put Bid");   
             
        // Add series1 to the xyplot:
        mySimpleXYPlot.addSeries(callBidVsStrike, randomLPFormatter2());
        mySimpleXYPlot.addSeries(putBidVsStrike, randomLPFormatter());
        mySimpleXYPlot.addSeries(callAskVsStrike, randomLPFormatter2());
        mySimpleXYPlot.addSeries(putAskVsStrike, randomLPFormatter());
 
 
        // By default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():
        mySimpleXYPlot.disableAllMarkup();
        
        mySimpleXYPlot.setDomainLabel("Strike Price ($)");
        mySimpleXYPlot.setRangeLabel("Bid/Ask Price ($)");
    }
    
    public LineAndPointFormatter randomLPFormatter() {
    	
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(
        		
                Color.RED, Color.RED, Color.RED);
        lpFormatter.setFillPaint(null);
        return lpFormatter;
    }
    
 public LineAndPointFormatter randomLPFormatter2() {
    	
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(
        		
                Color.BLACK, Color.BLACK, Color.BLACK);
        lpFormatter.setFillPaint(null);
        return lpFormatter;
    }
    

	
	// Called when user clicks menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }

    // Menu handler
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionsIcon:	
            	Intent myIntent = new Intent(GraphingActivity.this, OptionsActivity.class);
			   	GraphingActivity.this.startActivity(myIntent);
                break;
            case R.id.graphingIcon:  
            	Toast.makeText(this, "Already in Graphing View!", Toast.LENGTH_SHORT).show();   
                break;
            case R.id.helpIcon: 
            	Toast.makeText(this, "Launch Help Activity", Toast.LENGTH_SHORT).show();
                break;
            case R.id.exitIcon: 
            	java.lang.System.exit(0);                             
        }
        return true;
    }
    
    
    
}