package edu.illinois.cs125.cs125_finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.lang.Double;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import java.io.UnsupportedEncodingException;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Button;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import android.widget.EditText;

import java.util.Map;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


/*
    To Do

    Finish return for geo to country and zip

    Finish Maping to zip MAP


 */





public class MainActivity extends AppCompatActivity {
    private boolean disableWeatherDebug = false;
    /**
     * Because RequestQueue sucks and I need a way to return variables.
     */
    private static String tmpString = "";

    /**
     * Tag thing I have No Idea.
     */
    private static final String TAG = "Final Project:Main";


    /**
     * WTF. I dont even know how this works.
     */
    private static RequestQueue requestQueue;

    /**
     * Zipcode and ETA time for the zip code.
     *
     * dict
     * Key = Epoch Time
     * Value = "countryCode:ZIP" <- String
     */
    private Map<Double, String[]> zipCodes = new HashMap<Double, String[]>();

    /**
     * I wonder what this could be? Definitely not the time of departure in EpochTime.
     */
    private long departureTime;

    /**
     *  Zip Info
     */
    private Map<Double, String[]> WeatherInfo;

    /**
     *  Amount of travel time (seconds) threshold to subdivide into smaller segments.
     */
    private static int SUBDIVIDE_TRAVELTIME_THRESHOLD = 60*10;
    /**
     * the page user is on.
     */
    private int page = 0;

    private int completedSearches =0;
    private int maxWaypoints = 24;

    private int numberDisplays = 5;
    private List<TextView> weatherDisplays = new ArrayList<>();
    private List<TextView> iconDisplays = new ArrayList<>();
    private List<TextView> tempDisplays = new ArrayList<>();

    /**
     * Run when this activity comes to the foreground.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(this);

        setContentView(R.layout.activity_main);


        // Load the main layout for our activity


        weatherDisplays.add( (TextView)findViewById(R.id.Weather0) );
        weatherDisplays.add( (TextView)findViewById(R.id.Weather1) );
        weatherDisplays.add( (TextView)findViewById(R.id.Weather2) );
        weatherDisplays.add( (TextView)findViewById(R.id.Weather3) );
        weatherDisplays.add( (TextView)findViewById(R.id.Weather4) );

        iconDisplays.add( (TextView)findViewById(R.id.Icon0) );
        iconDisplays.add( (TextView)findViewById(R.id.Icon1) );
        iconDisplays.add( (TextView)findViewById(R.id.Icon2) );
        iconDisplays.add( (TextView)findViewById(R.id.Icon3) );
        iconDisplays.add( (TextView)findViewById(R.id.Icon4) );

        tempDisplays.add( (TextView)findViewById(R.id.Temp0) );
        tempDisplays.add( (TextView)findViewById(R.id.Temp1) );
        tempDisplays.add( (TextView)findViewById(R.id.Temp2) );
        tempDisplays.add( (TextView)findViewById(R.id.Temp3) );
        tempDisplays.add( (TextView)findViewById(R.id.Temp4) );

        final EditText startAddressText = findViewById(R.id.StartAddress);
        final EditText endAddressText = findViewById(R.id.EndAddress);
        final EditText startZipText = findViewById(R.id.startZip);
        final EditText endZipText = findViewById(R.id.endZip);

        //==========================================


        //menu.add(Menu.NONE, MENU_ITEM_ITEM1, Menu.NONE, "Item name");

        final Button nextPageBtn = findViewById(R.id.nextButton);
        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (page < completedSearches / 5) {
                    page++;
                }
                Log.d(TAG, WeatherInfo.toString());
                List<Double> timeKeys = new ArrayList<Double>();
                timeKeys.addAll(zipCodes.keySet());

                Collections.sort(timeKeys);


                for (int display = 0; display < numberDisplays; display++) {
                    if (display + 5 * page < completedSearches) {
                        weatherDisplays.get(display).setText("Weather at " + String.valueOf(page + display) + " Hour");
                        iconDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page + display))[1]);
                        tempDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page + display))[2]);
                    } else {
                        weatherDisplays.get(display).setText("");
                        iconDisplays.get(display).setText("");
                        tempDisplays.get(display).setText("");
                    }
                }
            }
        });
        final Button prevPageBtn = findViewById(R.id.prevButton);
        prevPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (page > 0) {
                    page--;
                }
                Log.d(TAG, WeatherInfo.toString());
                List<Double> timeKeys = new ArrayList<Double>();
                timeKeys.addAll(zipCodes.keySet());

                Collections.sort(timeKeys);


                for (int display = 0; display < numberDisplays; display++) {
                    if (display + 5 * page < completedSearches) {
                        weatherDisplays.get(display).setText("Weather at " + String.valueOf(page + display) + " Hour");
                        iconDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page + display))[1]);
                        tempDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page + display))[2]);
                    } else {
                        weatherDisplays.get(display).setText("");
                        iconDisplays.get(display).setText("");
                        tempDisplays.get(display).setText("");
                    }
                }

            }
        });

        final Button openFile = findViewById(R.id.button);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "Refreshing Web Page Data");

                String startAddr = startAddressText.getText().toString();
                String startZip = startZipText.getText().toString();
                String startCountry = "US";


                String endAddr = endAddressText.getText().toString();
                String endZip = endZipText.getText().toString();
                String endCountry = "US";

                if (startAddr.equals("Starting Address") && endAddr.equals("Destination Address")) {
                    Log.d(TAG, "Using Shortcut Input Cuz, We Lazy Testers");

                    startAddr    = "201 N Goodwin Ave";
                    startZip     = "61801";

                    //endAddr    = "233 S Wacker Dr";
                    //endZip     = "60606";

                    endAddr    = "8 Melia Way";
                    endZip     = "11746";
                }


                page = 0;
                departureTime = System.currentTimeMillis()/1000;
                departureTime += 3600 - departureTime%3600;// Round Up to the nearest Hour.

                WeatherInfo = new HashMap<Double, String[]>();
                getZipCodes(startAddr, startZip, startCountry, endAddr, endZip, endCountry);
                Log.d(TAG,String.valueOf(zipCodes.size()));
            }
        });
    }

    /**
     * Run when this activity is no longer visible.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Generate API LocationCode and forecast at the zip location and at the time.
     * @param latitude Latitude value N
     * @param longitude Longitude Value E
     * @param EpochDateTime The EpochDateTime For Forecast
     */
    private void getAPILocationCode(final String latitude, final String longitude, final double EpochDateTime) {
        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    "http://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey="+BuildConfig.AccuWeather_KEY+"&q="
                            +latitude+"%2C"+longitude,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            try {
                                //Log.d(TAG, response.toString(2));
                                String locationCode = response.getString("Key");
                                //Log.d(TAG, "LoctaionCode = "+locationCode);
                                weatherAPICall(locationCode, EpochDateTime);

                            } catch (JSONException ignored) {
                                Log.d(TAG, "HUGEO BIGO JSON APILocation Code Error");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.e(TAG, "Error getLocationCode: "+error.toString());
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the weather forecast for a max of 12 hr period.
     * @param LocationCode The API location code.
     * @param EpochDateTime The desired time of forecast in EpochDateTime.
     */
    void weatherAPICall(String LocationCode, final double EpochDateTime) {
        try {
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    "http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/"+LocationCode+"?apikey="+ BuildConfig.AccuWeather_KEY,
                    null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(final JSONArray hourlyForecasts) {
                            try {
                                //Log.d(TAG, hourlyForecasts.toString(2));
                                //Log.d(TAG, "Looking For " + String.valueOf(EpochDateTime));
                                for (int hourlyIndex = 0; hourlyIndex<hourlyForecasts.length(); hourlyIndex++) {
                                    int time = hourlyForecasts.getJSONObject(hourlyIndex).getInt("EpochDateTime");
                                    //Log.d(TAG, "found " + String.valueOf(time));

                                    if (time == EpochDateTime) {
                                        //Log.d(TAG, hourlyForecasts.getJSONObject(hourlyIndex).toString(2));
                                        String IconID = String.valueOf(hourlyForecasts.getJSONObject(hourlyIndex).getInt("WeatherIcon"));
                                        String Weather = hourlyForecasts.getJSONObject(hourlyIndex).getString("IconPhrase");
                                        String TempUnit = hourlyForecasts.getJSONObject(hourlyIndex).getJSONObject("Temperature").getString("Unit");
                                        String Temp = hourlyForecasts.getJSONObject(hourlyIndex).getJSONObject("Temperature").getString("Value");
                                        String Time = String.valueOf(EpochDateTime%3600)+":"+String.valueOf(EpochDateTime%60);
                                        String[] packagedData = {IconID, Weather, Temp+" "+TempUnit};
                                        WeatherInfo.put(EpochDateTime, packagedData );
                                        completedSearches++;
                                        if (completedSearches == zipCodes.size()) {
                                            Log.d(TAG, WeatherInfo.toString());
                                            List<Double> timeKeys = new ArrayList<Double>();
                                            timeKeys.addAll(zipCodes.keySet());

                                            Collections.sort(timeKeys);


                                            for (int display = 0; display < numberDisplays; display++) {
                                                if (display + numberDisplays * page < completedSearches) {
                                                    weatherDisplays.get(display).setText("Weather at " + String.valueOf(page*numberDisplays + display) + " Hour");
                                                    iconDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page*numberDisplays + display))[1]);
                                                    tempDisplays.get(display).setText(WeatherInfo.get(timeKeys.get(page*numberDisplays + display))[2]);
                                                } else {
                                                    weatherDisplays.get(display).setText("");
                                                    iconDisplays.get(display).setText("");
                                                    tempDisplays.get(display).setText("");
                                                }
                                            }
                                        }

                                    }
                                }
                            } catch (JSONException ignored) {
                                Log.d(TAG,"WeatherAPI JSON Error- Big No-No");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.e(TAG, error.toString());
                }
            });
            requestQueue.add(jsonArrayRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Docodes Google Lines For subdivision later.
     * Code Provided by:
     * http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * @param encoded
     * @return
     */
    private List<Double[]> decodePoly(String encoded) {
        List<Double[]> geoPoints = new ArrayList<Double[]>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            Double geoPoint[]= new Double[2];

            geoPoint[0] = (((double) lat / 1E5) );
            geoPoint[1] = (((double) lng / 1E5) );
            //Log.d(TAG, "Waypoint: Lat:"+ geoPoint[0].toString() +"   Long:"+geoPoint[1].toString());
            geoPoints.add(geoPoint);
        }

        return geoPoints;
    }

    /**
     * Source https://www.geodatasource.com/developers/java
     * https://stackoverflow.com/questions/36655207/why-calculating-distance-between-two-lat-long-gives-wrong-result
     * @param lat1 The latitude of point 1.
     * @param lon1 The longitude of point 1.
     * @param lat2 The latitude of point 2.
     * @param lon2 The longitude of point 2.
     * @return
     */
    private double geolocationDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = (lat2-lat1)*Math.PI/180.0;
        double dLon = (lon2-lon1)*Math.PI/180.0;
        lat1 = lat1*Math.PI/180.0;
        lat2 = lat2*Math.PI/180.0;

        double a = Math.sin(dLat/2.0) * Math.sin(dLat/2.0) +
                Math.sin(dLon/2.0) * Math.sin(dLon/2.0) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        return d*1000;
    }

    /**
     * Takes The Web API Directions and decodes them while extrapolationg points with about 1 hr intervals.
     * break up Steps
     * Break Up Polyline
     * breakUp line
     * @param directions JSON object from web.
     */
    private void decodeDirectionsToPoints(final JSONObject directions) {
        Log.d(TAG, "Decoding ZipCodes");
        int routeValue = 0;
        try {

            JSONObject route = directions.getJSONArray("routes").getJSONObject(routeValue).getJSONArray("legs").getJSONObject(0);
            JSONArray steps = route.getJSONArray("steps");
            zipCodes = new HashMap<Double, String[]>();

            int timeAccumilator = 0; // This is in seconds
            int hour = 0;

            // For Each Step
            for (int stepIndex =0; stepIndex<steps.length(); stepIndex++) {
                JSONObject step =  steps.getJSONObject(stepIndex);
                long stepTravelTime = step.getJSONObject("duration").getLong("value");
                //Log.d(TAG, "Step Travel Time: "+String.valueOf(stepTravelTime)+" Adding to Total of "+String.valueOf(timeAccumilator));

                if ((timeAccumilator + stepTravelTime) - (hour * 3600) >= 3600 + SUBDIVIDE_TRAVELTIME_THRESHOLD) {

                    //System.out.println( "Need to Break Step At...");
                    //System.out.println("Step Travel Time: "+String.valueOf(stepTravelTime)+" Adding to Total of "+String.valueOf(timeAccumilator));

                    List<Double[]> polylineRoute = decodePoly(step.getJSONObject("polyline").getString("points"));
                    double distance = step.getJSONObject("distance").getDouble("value");
                    double velocity = distance/stepTravelTime;
                    System.out.println("\tVelocity "+String.valueOf(velocity)+" Dist: "+String.valueOf(distance));

                    // For each line in the polyline
                    for (int locationIndex = 0; locationIndex< polylineRoute.size()-1; locationIndex++) {
                        double lat1 = polylineRoute.get(locationIndex)[0];
                        double lng1 = polylineRoute.get(locationIndex)[1];
                        double lat2 = polylineRoute.get(locationIndex+1)[0];
                        double lng2 = polylineRoute.get(locationIndex+1)[1];

                        double linedistance = geolocationDistance(lat1,lng1,lat2,lng2);
                        double subTravelTime = linedistance / velocity;
                        //System.out.println( "\t\tBreaking Up Poly, Line Travel Time "+String.valueOf(subTravelTime));

                        if ((timeAccumilator + subTravelTime) - hour * 3600 >= 3600 + SUBDIVIDE_TRAVELTIME_THRESHOLD) {
                            for (int subHour = 0; subHour < subTravelTime/3600; subHour++) { // 25 meters is to accomadate for lost precision;
                                double drivingTimeTillHour = 3600 - (timeAccumilator %3600);
                                Log.d(TAG, "\t\tBreaking Up Poly Line Ind. TargetSubSub Travel Time: "+String.valueOf(drivingTimeTillHour));

                                timeAccumilator += drivingTimeTillHour;
                                double fraction = drivingTimeTillHour/subTravelTime;
                                lat1 += (lat2-lat1)*fraction;
                                lng1 += (lng2-lng1)*fraction;
                                subTravelTime -= drivingTimeTillHour;


                                String[] latlong = {String.valueOf(lat1), String.valueOf(lng1)};
                                Double tmpTime = new Double(departureTime + 3600 * hour);
                                zipCodes.put(tmpTime, latlong);
                                hour++;
                            }

                        } else if ((timeAccumilator + subTravelTime) > hour * 3600) {
                            timeAccumilator += subTravelTime;
                            String[] latlong = {String.valueOf(lat2), String.valueOf(lng2)};
                            Double tmpTime = new Double(departureTime + 3600 * hour);
                            zipCodes.put(tmpTime, latlong);
                            hour++;
                        } else {
                            timeAccumilator += subTravelTime;
                        }
                    }

                } else if (timeAccumilator+stepTravelTime  - hour*3600 > 0) {
                    timeAccumilator += stepTravelTime;
                    String lat = String.valueOf(step.getJSONObject("end_location").getLong("lat"));
                    String lng = String.valueOf(step.getJSONObject("end_location").getLong("lng"));
                    String[] latlong = {lat, lng};
                    Double tmpTime = new Double(departureTime + 3600 * hour);
                    zipCodes.put(tmpTime, latlong);
                    hour++;
                } else {
                    timeAccumilator += stepTravelTime;
                }


            }
            hour++;
            JSONObject step =  steps.getJSONObject( steps.length()-1 );
            String lat = String.valueOf(step.getJSONObject("end_location").getLong("lat"));
            String lng = String.valueOf(step.getJSONObject("end_location").getLong("lng"));
            String[] latlong = {lat, lng};
            Double tmpTime = new Double(departureTime + 3600 * hour);
            zipCodes.put(tmpTime, latlong);
        } catch (JSONException ignored) {
            Log.d(TAG, "HUGE EXCEPTION");

        }
        Log.d(TAG, ""+zipCodes.values());
    }

    /**
     * conver latitude and longitude into zip and country codes for Weather API.
     * @param latitude latitude value.
     * @param longitude longitude value.
     */


    /**
     * Get the directions from web API and pull out Zip Codes and eta's later.
     * @param startAddr    This.
     * @param startZip     Is.
     * @param startCountry So.
     * @param endAddr      Very.
     * @param endZip       Self.
     * @param endCountry   Explanatory.
     * @return
     */
    void getZipCodes(String startAddr, String startZip, String startCountry,
                    String endAddr, String endZip, String endCountry) {
        Log.d(TAG, "Getting ZipCodes");
        String url = "https://maps.googleapis.com/maps/api/directions/json?";
        url += "origin="+startAddr+",+"+startZip+",+"+startCountry;
        url += "&destination="+endAddr+",+"+endZip+",+"+endCountry;
        url += "&key="+BuildConfig.Maps_KEY;

        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                             Log.d(TAG, "parsing ZipCodes");
                             decodeDirectionsToPoints(response);

                             Log.d(TAG,"Found "+String.valueOf(zipCodes.size())+" Way-points.");

                             completedSearches =0;
                             if (disableWeatherDebug) {
                                 return;
                             }
                             if (zipCodes.size() <= maxWaypoints) {
                                 for (Double locationKey : zipCodes.keySet()) {

                                     String[] tmpLocation = zipCodes.get(locationKey);
                                     String lat = tmpLocation[0];
                                     String lng = tmpLocation[1];

                                     getAPILocationCode(lat, lng, locationKey.doubleValue());

                                 }
                             } else {
                                 Log.d(TAG, "Too many Waypoints. You F*** Up Done Gud.");
                             }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.d(TAG, "Error Resp EXCEPTION");
                    Log.e(TAG, error.toString());
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}