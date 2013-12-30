package com.example.gps_project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.GraphPlaceListCallback;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphPlace;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;



@SuppressLint("HandlerLeak")
public class Gps_project extends FragmentActivity implements LocationListener{

	private static double latitude = 24.15027;
	private static double longitude = 120.685768;
	private ServerConnector connect;
	private ServerConnector Locusconnect;
	private boolean getService = false;
	private static final String MAP_URL = "file:///android_asset/googleMap.html"; //the url of html of google map	
	private WebView webView; //declare the webview for google map
	private LocationManager lms;
	private JSONArray buffer = new JSONArray();
	public static final String APP_ID = "144319489109749";
	private static TextView mText;
	private Session usersession = null;
	private static int p;
	private String facebookid = "";
	private ProgressDialog mDialog;





	final static int AUTHORIZE_ACTIVITY_RESULT_CODE = 0;
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions",  "user_status", "user_photos", "friends_photos", "publish_stream");


	private Handler mHandler =  new  Handler() {
		public  void  handleMessage (Message msg) {

			String buf = null;
			switch(msg.what){
				case 1: 
					//message_txt.setText((String)msg.obj);
					break;
				case 2:
					try {
						mDialog.dismiss();
						JSONArray checkinArray = new JSONArray(String.valueOf(msg.obj));
						LayoutInflater inflater = LayoutInflater.from(Gps_project.this); 
						final View v = inflater.inflate(R.layout.locationmenu, null);
						final ListView lv = (ListView)v.findViewById(R.id.locationmenu);	//stop point list
						
						/*Some information is store temporarily*/
						final Vector<String> tmp = new Vector<String>();
						final Vector<String> tem_ID = new Vector<String>();
						final Vector<String> time = new Vector<String>();
						final ArrayList<gpsinfo> gps = new ArrayList<gpsinfo>();
						Location requestlocation = new Location("User");

						
						p = checkinArray.length();	//the amount of start point receive from server
						for(int i=0 ; i<checkinArray.length() ; ++i){
							final JSONObject point = checkinArray.getJSONObject(i);

							requestlocation.setLatitude(point.getDouble("lat"));
							requestlocation.setLongitude(point.getDouble("lng"));
							
							//Six parameters, session, location, search radius, amount of return information, keywords and callback 
							Request.executePlacesSearchRequestAsync(Session.getActiveSession(), requestlocation, 100, 10, null, new GraphPlaceListCallback(){

								@Override
								public void onCompleted(
									List<GraphPlace> places,
									Response response) {
									// TODO Auto-generated method stub
									if(places != null && !places.isEmpty()){
										try {
											
											//Find out the attractions which are the nearest to the stop point
											int count = 0, min_place = 0;
											double min_dist = Double.MAX_VALUE;
											while(count < places.size()){
												if(D_jw(point.getDouble("lat"), point.getDouble("lng"), places.get(count).getLocation().getLatitude(), places.get(count).getLocation().getLongitude())*1000.00 <= min_dist){
													min_dist = D_jw(point.getDouble("lat"), point.getDouble("lng"), places.get(count).getLocation().getLatitude(), places.get(count).getLocation().getLongitude());
													min_place = count;
												}
												count++;
											}
											//The string will display on the list of stop point
											String buf = places.get(min_place).getName()+"\nStart_time:"+point.getString("startTime")+"\nEnd_time "+point.getString("endTime");
											tmp.add(buf);
											
											tem_ID.add(places.get(min_place).getId());
											gpsinfo tem_gps = new gpsinfo();
											
											tem_gps.set(point.getString("lat"), point.getString("lng"), point.getString("startTime"), point.getString("endTime"));
											gps.add(tem_gps);
											//The default message posted to the wall if the botton "懶人功能" is pressed
											String timestring = "從 "+point.getString("startTime")+" 到 "+point.getString("endTime");
											time.add(timestring);
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										//Wait until the last stop point is processed. Then make the listview
										if(tmp.size() == p){
											final String[] locationarray = (String[])tmp.toArray(new String[0]);
											lv.setAdapter(new ArrayAdapter<String>(Gps_project.this, R.layout.locationitem, R.id.location_textitem, locationarray));
											lv.setOnItemClickListener(new OnItemClickListener(){

												@Override
												public void onItemClick(AdapterView<?> arg0,
													View arg1, int arg2, long arg3) {
													// TODO Auto-generated method stub
													
													//start another activity, pass the location and time
													final Intent myIntent = new Intent(getApplicationContext(), Places.class);
													Bundle para = new Bundle();
													para.putString("accesstoken", Session.getActiveSession().getAccessToken());
													para.putDouble("Latitude", Double.parseDouble(gps.get(arg2).Lat));
													para.putDouble("Longitude", Double.parseDouble(gps.get(arg2).Lon));
													para.putString("Start", gps.get(arg2).Start);
													para.putString("End", gps.get(arg2).End);
													myIntent.putExtras(para);
													startActivity(myIntent);
												}});
											new AlertDialog.Builder(Gps_project.this).setTitle("list").setView(v).setPositiveButton("懶人功能", new DialogInterface.OnClickListener(){

												@Override
												public void onClick(
													DialogInterface arg0,
													int arg1) {
													// TODO Auto-generated method stub
													String message = "嘿嘿~"+mText.getText()+"到此一遊!!";
													Iterator<String> it = tem_ID.iterator();
													Iterator<String> itt = time.iterator();
													Bundle params = new Bundle();
													while(it.hasNext()){
														params.putString("message", message+"\n"+itt.next());
														params.putString("place", it.next());
														new RequestAsyncTask(new Request(Session.getActiveSession(), "me/feed", params, HttpMethod.POST, new Request.Callback() {

															@Override
															public void onCompleted(Response response) {
																// TODO Auto-generated method stub
																new AlertDialog.Builder(Gps_project.this).setTitle("123").setMessage(response.toString()).show();
															}
														})).execute();
													}

													}


											}).show();
										}
									}
									else
										p--;
									}

							});
							String centerURL = "javascript:centerAt(" +
								point.getString("lat") + "," +
								point.getString("lng")+ ")";
							webView.loadUrl(centerURL);

							String markURL = "javascript:mark(" +
								point.getString("lat") + "," +
								point.getString("lng")          + ")";
							webView.loadUrl(markURL);
						}					
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}


					break;
				//handle the request of drawing locus on the map
				case 3:
					try {
						mDialog.dismiss();
						
						JSONArray locusArray = new JSONArray(String.valueOf(msg.obj));
						for(int i=0 ; i<locusArray.length() ; ++i){
							final JSONObject point = locusArray.getJSONObject(i);
							if(i%10 == 0){
								String addLocusURL = "javascript:markLocus(" +
									point.getString("lat") + "," +
									point.getString("lng")+ ")";
								webView.loadUrl(addLocusURL);
							}
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				default: break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gps_test);



		mText = (TextView) findViewById(R.id.txt);									//display client's information
		final Button facebookButton = (Button)findViewById(R.id.facebookbutton);	//Login button
		Button friendbutton = (Button)findViewById(R.id.button);
		friendbutton.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				final Intent myIntent = new Intent(getApplicationContext(), Places.class);

				Bundle para = new Bundle();
				para.putString("accesstoken", Session.getActiveSession().getAccessToken());
				para.putDouble("Latitude", 24.15027);
				para.putDouble("Longitude", 120.685768);
				para.putString("Start", "2013-11-26 07:00:01");
				para.putString("End", "2013-12-31 08:00:01");
				myIntent.putExtras(para);
				startActivity(myIntent);
			}

		});



		LocationManager status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));	//open an Android location manager
		setupWebView();//load Webview
		
		//detect whether the GPS service is opened or not
		if (status.isProviderEnabled(LocationManager.GPS_PROVIDER) || status.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			getService = true;
			locationServiceInitial();
		} else {
			Toast.makeText(this, "Please open the GPS service!!", Toast.LENGTH_LONG).show();
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		}


		facebookButton.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				Session.openActiveSession(Gps_project.this, true, new Session.StatusCallback() {

					// callback when session changes state
					@Override
					public void call(final Session session, SessionState state, Exception exception) {
						if (session.isOpened()) {
							usersession = session;


							// make request to the /me API
							Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

								// callback after Graph API response with user object
								@Override
								public void onCompleted(GraphUser user, Response response) {
									if (user != null) {
										try{
											ProfilePictureView profilePictureView = (ProfilePictureView)findViewById(R.id.user_pic);
											profilePictureView.setProfileId(user.getId());
											mText.setText("Hi " + user.getName() + "!!");
											facebookid = user.getId();

										}catch(Exception e){
											mText.setText(e.getMessage());
										}

									}
									else mText.setText("error");
								}
							});
						}
					}
				});

			}

		});


		final Button getCheckin = (Button)findViewById(R.id.getCheckin);//declare a button for getting the node that should be checked in
		getCheckin.setOnClickListener(new Button.OnClickListener(){ // set a button listener for get the array of checkin-node 
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				//show the processing dialog
				mDialog = new ProgressDialog(Gps_project.this);
				mDialog.setMessage("Please wait...");
				mDialog.setCancelable(true);
				mDialog.show();
				
				//use user's facebook id as database lookup identifier
				connect = new ServerConnector("100000388583491");
				new Thread (runRequest).start();
				Locusconnect = new ServerConnector("100000388583491");
				new Thread (runGetLocus).start();
			}
		});

	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	private void enable(){
		if(getService) {
			//lms.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0,this);
			lms.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100000, 0,this);
		}
	}
	
	/** Sets up the WebView object and loads the URL of the page **/
	@SuppressLint("SetJavaScriptEnabled")
	private void setupWebView(){
		webView = (WebView) findViewById(R.id.google_map);
		//enable the javascript of webView
		webView.getSettings().setJavaScriptEnabled(true);
		// enable javascript call class FromJavaScript
		webView.addJavascriptInterface(new FromJavaScript(this), "Android");
		//loading URL
		webView.loadUrl(MAP_URL);   
	}

	//private String bestProvider = LocationManager.GPS_PROVIDER;

	private void locationServiceInitial() {

		// TODO Auto-generated method stub

		lms = (LocationManager) getSystemService(LOCATION_SERVICE);
		enable();

	}


	/*當位置改變的時候*/
	@Override
	public void onLocationChanged(Location location) {

		// TODO Auto-generated method stub

		getLocation(location);
		if (location !=null){    
			//將畫面移至定位點的位置，呼叫在googlemaps.html中的centerAt函式
			final String centerURL = "javascript:centerAt(" +
				location.getLatitude() + "," +
				location.getLongitude()+ ")";
			webView.loadUrl(centerURL);

			if(location.getProvider().equals("gps")){
				final String markURL = "javascript:mark1(" +
					location.getLatitude() + "," +
					location.getLongitude()+ ")";
				webView.loadUrl(markURL);
			}
			else{
				final String markURL = "javascript:mark1(" +
					location.getLatitude() + "," +
					location.getLongitude()+ ")";
				webView.loadUrl(markURL);
			}
		}

	}



	/*當GPS或是網路定位功能關閉時*/

	@Override

	public void onProviderDisabled(String provider) {

		// TODO Auto-generated method stub
		Toast.makeText(this, "Provider " + provider + " disabled", Toast.LENGTH_LONG).show();
	}
	/*當GPS或是網路定位功能開啟時*/

	@Override

	public void onProviderEnabled(String provider) {

		// TODO Auto-generated method stub
		Toast.makeText(this, "Get "+ provider  +" provider", Toast.LENGTH_LONG).show();
	}
	/*定位狀態改變時*/

	@Override

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		Toast.makeText(this, provider + " status changed: " + String.valueOf(status), Toast.LENGTH_LONG).show();
	}

	//when gpsListener get location node
	@SuppressLint("SimpleDateFormat")
	private void getLocation(Location location) {

		// TODO Auto-generated method stub

		if(location != null){

			longitude = location.getLongitude(); //蝬漲get
			latitude = location.getLatitude();   //蝺臬漲get

			float speed = location.getSpeed();
			TextView sp = (TextView) findViewById(R.id.speed);                     
			String time_str = Long.toString(location.getTime()); //get time and convert to string
			Date date = new Date(Long.parseLong(time_str.trim()));//Create a new Date object
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss"); //transform the time into format
			String dateString = formatter.format(date);//the string which is transformed to proper format
			sp.setText(Float.toString(speed));
			sp.setTextSize(24);
			
			//store the locus in array
			JSONObject jsonObj = new JSONObject();
			try { 		
				jsonObj.put("longitude", longitude);
				jsonObj.put("latitude", latitude);
				jsonObj.put("time", dateString);
				buffer.put(jsonObj);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//if network is connected ,upload data and renew the array
			if(isConnectInternet() && !facebookid.equals("")){ 		
				connect = new ServerConnector(facebookid,buffer);
				new Thread (runnable).start();
				buffer = new JSONArray();       	
			}
		}else{
			Toast.makeText(this, "Can't get your location!!", Toast.LENGTH_LONG).show();
		}
	}

	Runnable runnable = new Runnable(){  //create a runnable for sending request of http 
		public void run(){
			Message message;
			message = mHandler.obtainMessage(1,connect.sendLocation());
			mHandler.sendMessage(message);
		}
	};

	Runnable runRequest = new Runnable(){  //create a runnable for sending request of http 
		public void run(){
			Message message;
			String result = connect.getCheckinNode();
			message = mHandler.obtainMessage(2,result);
			mHandler.sendMessage(message);
		}
	};

	Runnable runGetLocus = new Runnable(){  //create a runnable for sending request of http 
		public void run(){
			Message message;
			message = mHandler.obtainMessage(3,Locusconnect.getLocus());
			mHandler.sendMessage(message);
		}
	};

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Session s = Session.getActiveSession();
		usersession = s;

		if(s != null){
			List<String> permissions = s.getPermissions();
			//Toast.makeText(this, Boolean.toString(isSubsetOf(PERMISSIONS, permissions)), Toast.LENGTH_LONG).show();

			if (!isSubsetOf(PERMISSIONS, permissions)) {
				s.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSIONS));
			}

			Request.executeMeRequestAsync(s, new Request.GraphUserCallback() {

				// callback after Graph API response with user object
				@Override
				public void onCompleted(GraphUser user, Response response) {
					if (user != null) {
						try{
							ProfilePictureView profilePictureView = (ProfilePictureView)findViewById(R.id.user_pic);
							profilePictureView.setProfileId(user.getId());
							mText.setText("Hi " + user.getName() + "!!");
							facebookid=user.getId();
						}catch(Exception e){
							mText.setText(e.getMessage());
						}

					}
					else mText.setText("error");
				}
			});
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	//compute the distance of two coordinates
	public static double D_jw(double wd1,double jd1,double wd2,double jd2){
		double x,y,out;
		double PI=3.14159265;
		double R=6.371229*1e6;

		x=(jd2-jd1)*PI*R*Math.cos( ((wd1+wd2)/2) *PI/180)/180;
		y=(wd2-wd1)*PI*R/180;
		out=Math.hypot(x,y);
		return out/1000;
	}
	
	//check if network is available
	public boolean isConnectInternet() {	
		ConnectivityManager conManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
		if(networkInfo != null){ 
			return networkInfo.isAvailable();
		}
		return false;
	}

	private class FromJavaScript		//class for communication with google map
	{
		private Context c;
		FromJavaScript(Context c) 
		{
			this.c = c;
		}
		@JavascriptInterface
		public void showMsg(String data) //get the message of google map
		{
			//Log.v("123", data);
			Message message;
			message = mHandler.obtainMessage(1,data);
			mHandler.sendMessage(message);

		}
	}

	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
		for (String string : subset) {
			if (!superset.contains(string)) {
				return false;
			}
		}
		return true;
	}

}

//the struct of start point
class gpsinfo{
	String Lat;
	String Lon;
	String Start;
	String End;

	public void set(String lat, String lon, String start, String end){
		Lat = lat;
		Lon = lon;
		Start = start;
		End = end;
	}
}

