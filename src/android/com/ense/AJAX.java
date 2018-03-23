package com.ense;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by clydeshaffer on 1/3/18.
 */

public class AJAX {

    public static Map<String, String> m(String... params) {
        Map result = new HashMap<String, String>();
        for(int i = 0; i < params.length-1; i+= 2) {
            result.put(params[i], params[i+1]);
        }
        return result;
    }

    public static enum Method {
        GET,
        POST,
        PUT,
        DELETE
    }

    public static interface X{
        void success(final int code, final String data);
        void failure(final int code, final String data);
    }

    private static class WebArgs {
        public String url;
        public Map<String, String> params;
        public X callback;
        public Method method;
        WebArgs(Method _method, String _url, Map<String,String> _params, X _callback) {
            method = _method;
            url = _url;
            params = _params;
            callback = _callback;
        }
    }

    public static void post(String url, Map<String, String> params, X callback) {
        new HttpsTask().execute(new WebArgs(Method.POST, url, params, callback));
    }

    public static void get(String url, Map<String, String> params, X callback) {
        new HttpsTask().execute(new WebArgs(Method.GET, url, params, callback));
    }

    private static String getQuery(Map<String, String> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;



        for (Map.Entry<String, String> pair : params.entrySet())
        {
            if (first)
                first = false;
            else
                result.append("&");


            result.append(URLEncoder.encode(pair.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private static class HttpsTask extends AsyncTask<WebArgs, Integer, String> {

        @Override
        protected String doInBackground(WebArgs... args_list) {
            for(WebArgs args : args_list) {
                try {
                    //TODO: move to a dedicated network manager class
                    URL url = new URL(args.url);
                    if(args.method == Method.GET && args.params != null) {
                        url = new URL(args.url + "?" + getQuery(args.params));
                    }
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    Log.d("ensereq", args.method.name());
                    conn.setRequestProperty("User-Agent", "ENSE_ANDROID/" + Build.VERSION.RELEASE);
                    conn.setUseCaches(false);
                    conn.setDoInput(true);

                    if(args.method != Method.GET) {
                        conn.setDoOutput(true);
                    }
                    conn.setRequestMethod(args.method.name().trim());

                    if(args.method != Method.GET) {

                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        if(args.params != null) {
                            writer.write(getQuery(args.params));
                        }
                        writer.flush();
                        writer.close();
                        os.close();
                    }

                    Log.i("method", conn.getRequestMethod());
                    int responseCode = conn.getResponseCode();


                    //Send result to callback func
                    if (responseCode < 300) {
                        String response = "";
                        String line;
                        BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line=br.readLine()) != null) {
                            response+=line;
                        }
                        final String completeResponse = response;
                        args.callback.success(responseCode, completeResponse);
                    }
                    else {
                        String response = "";
                        String line;
                        BufferedReader br=new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        while ((line=br.readLine()) != null) {
                            response+=line;
                        }
                        final String completeResponse = response;
                        args.callback.failure(responseCode, completeResponse);
                    }

                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                } catch (ProtocolException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    args.callback.failure(0, null);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }
    }


}
