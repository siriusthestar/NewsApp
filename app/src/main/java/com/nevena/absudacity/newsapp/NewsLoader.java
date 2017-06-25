package com.nevena.absudacity.newsapp;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class NewsLoader extends AsyncTaskLoader<List<NewsItem>> {

    public static final int NO_ERROR = 0;
    private static final String QUERY_URL = "https://content.guardianapis.com/search?q=%22artificial%20intelligence%22&api-key=test";

    private static int resultsPerPage = NewsActivity.NUMBER_OF_ITEMS_ON_SCREEN * 2; // init with double page size, will be reset after first load

    private int errorMessageResourceId; //0=OK, otherwise error message resource Id
    private ConnectivityManager connectivityManager;

    private int pageNumber = 1; // start with page 1, incremented post page load

    private boolean resultsExhausted = false; // indicator that there are no more results set when query return zero results


    public NewsLoader(Context context) {
        super(context);
        // Get a reference to the ConnectivityManager to check state of network connectivity in isOnline method
        connectivityManager = (ConnectivityManager) ((NewsActivity) context).getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isResultsExhausted() {
        return resultsExhausted;
    }

    public int getErrorMessageResourceId() {
        return errorMessageResourceId;
    }

    // request load items
    public void loadMore() {
        if (!resultsExhausted) { // do note perform query if there are no more results ( may not be required, handled in NewsActivity.loadItems() )
            forceLoad();
        }
    }

    @Override
    public List<NewsItem> loadInBackground() {

        List<NewsItem> results = new ArrayList<>();

        if (resultsExhausted) return results;

        errorMessageResourceId = NO_ERROR; // clear previous error, assume that is all good

        if (!isOnline()) {
            // return empty list and provide error message resource id
            errorMessageResourceId = R.string.no_internet_connection;
            return results;
        }

        try { // try/catch block for I/O errors
            URL url = new URL(QUERY_URL + "&page-size=" + resultsPerPage + "&page=" + pageNumber); // encode query string
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            try { // try-finally: ensure disconnect

                httpURLConnection.setReadTimeout(10000 /* milliseconds */);
                httpURLConnection.setConnectTimeout(15000 /* milliseconds */);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                StringBuilder stringBuilder = new StringBuilder();

                // reader will allow reading lines of text instead of bytes from stream
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                try { // try-finally: ensure reader close
                    String line;
                    while ((line = bufferedReader.readLine()) != null) { // read complete stream
                        stringBuilder.append(line); // ignore line breaks
                    }
                } finally {
                    bufferedReader.close();
                }

                try { // try-catch: JSON parser errors

                    JSONObject jsonResults = new JSONObject(stringBuilder.toString());
                    if (!jsonResults.has("response")) { // response may not exist, return empty list (not an error)
                        return results;
                    }
                    jsonResults = jsonResults.getJSONObject("response");
                    if (!jsonResults.has("results")) { // response may not exist, return empty list (not an error)
                        return results;
                    }
                    JSONArray items = jsonResults.getJSONArray("results");
                    for (int i = 0; i < items.length(); i++) {

                        JSONObject entry = items.getJSONObject(i);

                        String webTitle = null; // will allow webTitle not to exist
                        if (entry.has("webTitle")) webTitle = entry.getString("webTitle");

                        String sectionName = null; // will allow sectionName not to exist
                        if (entry.has("sectionName")) sectionName = entry.getString("sectionName");

                        String webUrl = null; // will allow webUrl not to exist
                        if (entry.has("webUrl")) webUrl = entry.getString("webUrl");

                        results.add(new NewsItem(webTitle, sectionName, webUrl)); // create new news item and add it to results
                    }

                    resultsExhausted = results.size() < NewsActivity.NUMBER_OF_ITEMS_ON_SCREEN; // set if query has returned less than one page of results

                    // increase page number, calculated from current resultsPerPage value because first request loads twice the amount of items that fit on screen
                    pageNumber += resultsPerPage / NewsActivity.NUMBER_OF_ITEMS_ON_SCREEN;

                    // reset number of results per page to number of items on screen (relevant only after first request)
                    resultsPerPage = NewsActivity.NUMBER_OF_ITEMS_ON_SCREEN;

                } catch (JSONException jsone) {
                    // error in JSON parsing
                    errorMessageResourceId = R.string.parse_error;
                    return results;
                }
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (IOException ioe) {
            // Error in networking
            errorMessageResourceId = R.string.io_error;
            return results;
        }
        return results;
    }

    public boolean isOnline() {

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        // If there is a network connection, fetch data
        return networkInfo != null && networkInfo.isConnected();

    }

}
