package edu.illinois.cs125.cs125_finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.lang.Double;
import java.util.List;
import java.util.ArrayList;

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
import android.widget.Button;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

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
     * Run when this activity comes to the foreground.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(this);

        setContentView(R.layout.activity_main);


        //final TextView textBox = findViewById(R.id.weatherOutput);
        final TextView weather1 = (TextView)findViewById(R.id.textView1);
        final TextView weather2 = (TextView)findViewById(R.id.textView2);
        final TextView weather3 = (TextView)findViewById(R.id.textView3);
        final TextView weather4 = (TextView)findViewById(R.id.textView4);

        weather1.setText("Rainy");
        weather2.setText("Sunny");
        weather3.setText("Rainy");
        weather4.setText("Cloudy");

        final Button openFile = findViewById(R.id.RefreshPage);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "Refreshing Web Page Data");


                String startAddr    = "201 N Goodwin Ave";
                String startZip     = "61801";
                String startCountry = "US";

                String endAddr    = "233 S Wacker Dr";
                String endZip     = "60606";
                String endCountry = "US";

                departureTime = System.currentTimeMillis()/1000;
                departureTime += 3600 - departureTime%3600;// Round Up to the nearest Hour.
                Log.d(TAG, "Beginning");
                getZipCodes(startAddr, startZip, startCountry,
                                            endAddr, endZip, endCountry);
                System.out.println(zipCodes.values());


            }
        });
        Log.d(TAG, "Refreshing Web Page Data");
        String startAddr    = "201 N Goodwin Ave";
        String startZip     = "61801";
        String startCountry = "US";

        String endAddr    = "233 S Wacker Dr";
        String endZip     = "60606";
        String endCountry = "US";

        departureTime = System.currentTimeMillis()/1000;
        departureTime += 3600 - departureTime%3600;// Round Up to the nearest Hour.

        Log.d(TAG, "Beginning");
        WeatherInfo = new HashMap<Double, String[]>();
        getZipCodes(startAddr, startZip, startCountry,
                endAddr, endZip, endCountry);
        Log.d(TAG, "Done");
        Log.d(TAG,String.valueOf(zipCodes.size()));
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
                                Log.d(TAG, "LoctaionCode = "+locationCode);
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
                                Log.d(TAG, "Looking For " + String.valueOf(EpochDateTime));
                                for (int hourlyIndex = 0; hourlyIndex<hourlyForecasts.length(); hourlyIndex++) {
                                    int time = hourlyForecasts.getJSONObject(hourlyIndex).getInt("EpochDateTime");
                                    Log.d(TAG, "found " + String.valueOf(time));

                                    if (time == EpochDateTime) {
                                        Log.d(TAG, hourlyForecasts.getJSONObject(hourlyIndex).toString(2));
                                        String IconID = String.valueOf(hourlyForecasts.getJSONObject(hourlyIndex).getInt("WeatherIcon"));
                                        String Weather = hourlyForecasts.getJSONObject(hourlyIndex).getString("IconPhrase");
                                        String TempUnit = hourlyForecasts.getJSONObject(hourlyIndex).getJSONObject("Temperature").getString("Unit");
                                        String Temp = hourlyForecasts.getJSONObject(hourlyIndex).getJSONObject("Temperature").getString("Value");
                                        String Time = String.valueOf(EpochDateTime%3600)+":"+String.valueOf(EpochDateTime%60);
                                        String[] packagedData = {IconID, Weather, Temp+" "+TempUnit};
                                        WeatherInfo.put(EpochDateTime, packagedData );
                                        break;
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

            geoPoint[0] = (((double) lat / 1E5) * 1E6);
            geoPoint[1] = (((double) lng / 1E5) * 1E6);
            Log.d(TAG, "Waypoint: Lat:"+ geoPoint[0].toString() +"   Long:"+geoPoint[1].toString());
            geoPoints.add(geoPoint);
        }

        return geoPoints;
    }

    /**
     * Source https://www.geodatasource.com/developers/java
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @param unit
     * @return
     */
    private double geolocationDistance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts decimal degrees to radians						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts radians to decimal degrees						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
    /**
     * Takes The Web API Directions and decodes them while extrapolationg points with about 1 hr intervals.
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
            for (int stepIndex =0; stepIndex<steps.length(); stepIndex++) {
                JSONObject step =  steps.getJSONObject(stepIndex);
                long travelTime = step.getJSONObject("duration").getLong("value");
                // If Travel Time Will Cause Overflow greater then SUBDIVIDE_TRAVELTIME_THRESHOLD
                if ((timeAccumilator + travelTime) - hour * 3600 >= 3600 + SUBDIVIDE_TRAVELTIME_THRESHOLD) {
                    String polyline = step.getJSONObject("polyline").getString("points");
                    List<Double[]> detailedRoute = decodePoly(polyline);

                    // meters per second
                    long velocity = step.getJSONObject("distance").getInt("value")/travelTime;

                    for (int locationIndex = 0; locationIndex< detailedRoute.size()-1; locationIndex++) {
                        double lat1 = detailedRoute.get(locationIndex)[0];
                        double lng1 = detailedRoute.get(locationIndex)[1];
                        double lat2 = detailedRoute.get(locationIndex+1)[0];
                        double lng2 = detailedRoute.get(locationIndex+1)[1];

                        double distance = geolocationDistance(lat1,lng1,lat2,lng2,"K")*1000;
                        double subTravelTime = distance * velocity;
                        if ((timeAccumilator + subTravelTime) - hour * 3600 >= 3600 + SUBDIVIDE_TRAVELTIME_THRESHOLD) {
                            double drivingDistLeft = distance;
                            while (drivingDistLeft > 25) { // 25 meters is to accomadate for lost precision;
                                double drivingTimeTillHour = 3600 - (timeAccumilator %3600);
                                travelTime += drivingTimeTillHour;
                                double fraction = drivingTimeTillHour*velocity/drivingDistLeft;
                                lat1 += (lat2-lat1)*fraction;
                                lng1 += (lng2-lng1)*fraction;
                                drivingDistLeft -= travelTime*velocity;

                                String[] latlong = {String.valueOf(lat1), String.valueOf(lng1)};
                                Double tmpTime = new Double(departureTime + 3600 * hour);
                                zipCodes.put(tmpTime, latlong);
                                Log.d(TAG, "Adding To ZIPS "+tmpTime.toString()+":"+latlong);

                                hour++;
                            }
                            Log.d(TAG, "Exiting LOOP");
                        // If Travel Time Will Cause Overflow
                        } else if ((timeAccumilator + subTravelTime) > hour * 3600) {
                            travelTime += subTravelTime;
                            String[] latlong = {String.valueOf(lat2), String.valueOf(lng2)};
                            Double tmpTime = new Double(departureTime + 3600 * hour);
                            zipCodes.put(tmpTime, latlong);
                            Log.d(TAG, "Adding To ZIPS "+tmpTime.toString()+":"+latlong);
                            hour++;
                        // If there is no overflow, just add the time.
                        } else {
                            travelTime += subTravelTime;
                        }


                    }


                // If Travel Time Will Cause Overflow
                } else if (timeAccumilator+travelTime  - hour*3600 > 0) {
                    timeAccumilator += travelTime;
                    String lat = String.valueOf(step.getJSONObject("end_location").getLong("lat"));
                    String lng = String.valueOf(step.getJSONObject("end_location").getLong("lng"));
                    String[] latlong = {lat, lng};
                    Double tmpTime = new Double(departureTime + 3600 * hour);
                    zipCodes.put(tmpTime, latlong);
                    Log.d(TAG, "Adding To ZIPS "+tmpTime.toString()+":"+latlong);
                    hour++;
                // If there is no overflow, just add the time.
                } else {
                    timeAccumilator += travelTime;
                }


            }
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

                             Log.d(TAG,"Found "+String.valueOf(zipCodes.size())+" Way-points");

                             for (Double locationKey : zipCodes.keySet()) {

                                String[] tmpLocation = zipCodes.get(locationKey);
                                String lat = tmpLocation[0];
                                String lng = tmpLocation[1];

                                getAPILocationCode(lat,lng,locationKey.doubleValue());

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