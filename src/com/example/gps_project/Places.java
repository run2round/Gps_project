/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.gps_project;

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.Request;
import com.facebook.Request.GraphPlaceListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphPlace;

@SuppressWarnings("deprecation")
public class Places extends Activity implements OnItemClickListener {
    
    private JSONObject location;

    protected ListView placesList;
    protected LocationManager lm;
    private static final double EARTH_RADIUS = 6378.137;

    protected static JSONArray jsonArray;
    static double LATITUDE = 23.002;
    static double LONGITUDE = 120.1992;
    protected ProgressDialog dialog;
    private Session session;
    private String tagID = "";
    Bundle params = new Bundle();

   
    
    private Handler mHandler =  new  Handler() {
    	public  void  handleMessage (Message msg) {
    		switch(msg.what){
    			
    		}
    	}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        location = new JSONObject();

        setContentView(R.layout.places_list);

        Bundle extras = getIntent().getExtras();
        double a , b;
        //get the bundle
        LATITUDE = extras.getDouble("Latitude");
        LONGITUDE = extras.getDouble("Longitude");
        params.putString("Start", extras.getString("Start"));
        params.putString("End", extras.getString("End"));
        session = Session.openActiveSessionWithAccessToken(Places.this, AccessToken.createFromExistingAccessToken(extras.getString("accesstoken"), null, null, null, null), new Session.StatusCallback() {
			
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				// TODO Auto-generated method stub
				
			}
		});
        fetchPlaces();
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * User returning from the Location settings menu. try to fetch location
         * again.
         */
    	switch(requestCode){
    	
    	default:
    		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    		break;
    }
    }
    
    
    
    /*
     * Fetch nearby places by providing the search type as 'place' within 1000
     * mtrs of the provided lat & lon
     */
    private void fetchPlaces() {
        
        Location requestlocation = new Location("User");
        requestlocation.setLatitude(LATITUDE);
        requestlocation.setLongitude(LONGITUDE);
        
        Request.executePlacesSearchRequestAsync(session, requestlocation, 100, 20, null, new GraphPlaceListCallback(){

			@Override
			public void onCompleted(List<GraphPlace> places, Response response) {
				// TODO Auto-generated method stub
				Iterator<GraphPlace> it = places.iterator();
				GraphPlace tem = null;
				jsonArray = new JSONArray();
				int i = 0;
				try{
					while(it.hasNext()){
						tem = it.next();
						jsonArray.put(i, new JSONObject().put("name", tem.getName()).put("id", tem.getId()).put("location", new JSONObject()
								.put("street", tem.getLocation().getStreet()).put("city", tem.getLocation().getCity()).put("state", tem.getLocation().getState())
								.put("lat", tem.getLocation().getLatitude()).put("lon", tem.getLocation().getLongitude())));
						i++;
					}
					
				}catch(JSONException e){}
				mHandler.post(new Runnable() {
	                @Override
	                public void run() {
	                    placesList = (ListView) findViewById(R.id.places_list);
	                    placesList.setOnItemClickListener(Places.this);
	                    placesList.setAdapter(new PlacesListAdapter(Places.this));
	                }
	            });
			}
        	
        });
    }
    

    
    @Override
    public void onItemClick(AdapterView<?> arg0, View v, final int position, long arg3) {
        
        		try{
                new AlertDialog.Builder(this).setTitle(R.string.check_in_title)
                        .setMessage(String.format(getString(R.string.check_in_at), jsonArray.getJSONObject(position).getString("name")))
                        .setPositiveButton(R.string.checkin, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            	String placeID = "";
                            	final JSONObject coordinate = new JSONObject();
								try {
									placeID = jsonArray.getJSONObject(position).getString("id");
									coordinate.put("latitude", jsonArray.getJSONObject(position).getJSONObject("location").getString("lat"));
									coordinate.put("longitude", jsonArray.getJSONObject(position).getJSONObject("location").getString("lon"));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
			                	params.putString("place", placeID);
			                	params.putString("accesstoken", Session.getActiveSession().getAccessToken());
								params.putString("coordinates", coordinate.toString());
			                	Intent myIntent = new Intent(getApplicationContext(), CheckinPage.class);
			                	myIntent.putExtras(params);
			    	            startActivity(myIntent);
                            }
                        }).setNegativeButton(R.string.cancel, null).show();
        		} catch (JSONException e) {
	                showToast("Error: " + e.getMessage());
			    }
        //}
    }
    
    
	
    public void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(Places.this, msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    /**
     * Definition of the list adapter
     */
    public class PlacesListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        Places placesList;

        public PlacesListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return jsonArray.length();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
        //Setting the place list
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            JSONObject jsonObject = null;
            try {
                jsonObject = jsonArray.getJSONObject(position);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            View hView = convertView;
            if (convertView == null) {
                hView = mInflater.inflate(R.layout.place_item, null);
                ViewHolder holder = new ViewHolder();
                holder.name = (TextView) hView.findViewById(R.id.place_name);
                holder.location = (TextView) hView.findViewById(R.id.place_location);
                holder.distance = (TextView) hView.findViewById(R.id.distance);
                hView.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) hView.getTag();
            try {
                holder.name.setText(jsonObject.getString("name"));
            } catch (JSONException e) {
                holder.name.setText("");
            }
            holder.name.setTextSize(24);
            
            try {
                String location = jsonObject.getJSONObject("location").getString("street") + ", "
                        + jsonObject.getJSONObject("location").getString("city") + ", "
                        + jsonObject.getJSONObject("location").getString("state");
                holder.location.setText(location);
            } catch (JSONException e) {
                holder.location.setText("No address");
            }
            
            try{
            	double place_lat, place_lon, dist;
            	place_lat = jsonObject.getJSONObject("location").getDouble("lat");
            	place_lon = jsonObject.getJSONObject("location").getDouble("lon");
            	dist = D_jw(place_lat, place_lon, LATITUDE, LONGITUDE);
            	holder.distance.setText("Distance: "+Integer.toString((int)(dist*1000))+" m.");
            } catch (JSONException e){
            	holder.distance.setText("distance error");
            }
            return hView;
        }

    }

    class ViewHolder {
        TextView name;
        TextView location;
        TextView distance;
    }
    
    private static double rad(double d){
    	return d * Math.PI / 180.0;
    }

    public static double GetDistance(double lat1, double lng1, double lat2, double lng2){
    	
	    double radLat1 = rad(lat1);
	    double radLat2 = rad(lat2);
	    double a = radLat1 - radLat2;
	    double b = rad(lng1) - rad(lng2);
	    double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
	    Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
	    s = s * EARTH_RADIUS;
	    s = Math.round(s * 10000) / 10000;
	    return s;
    }
    
    public static double D_jw(double wd1,double jd1,double wd2,double jd2){
	    double x,y,out;
	    double PI=3.14159265;
	    double R=6.371229*1e6;
	
	    x=(jd2-jd1)*PI*R*Math.cos( ((wd1+wd2)/2) *PI/180)/180;
	    y=(wd2-wd1)*PI*R/180;
	    out=Math.hypot(x,y);
	    return out/1000;
    }
}
