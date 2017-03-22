package com.locator.vishnu_grocery.myapplication;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
 
/**
 * This class encapsulates methods for requesting a server via HTTP GET/POST and
 * provides methods for parsing response from the server.
 *
 * @author www.codejava.net
 *
 */
public class HttpUtility {
 
    /**
     * Represents an HTTP connection
     */
    private static HttpURLConnection httpConn;
 
    /**
     * Makes an HTTP request using GET method to the specified URL.
     *
     * @param requestURL
     *            the URL of the remote server
     * @return An HttpURLConnection object
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static HttpURLConnection sendGetRequest(String requestURL)
            throws IOException {
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
 
        httpConn.setDoInput(true); // true if we want to read server's response
        httpConn.setDoOutput(false); // false indicates this is a GET request
 
        return httpConn;
    }
 
    /**
     * Makes an HTTP request using POST method to the specified URL.
     *
     * @param requestURL
     *            the URL of the remote server
     * @param params
     *            A map containing POST data in form of key-value pairs
     * @return An HttpURLConnection object
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static HttpURLConnection sendPostRequest(String requestURL,
            Map<String, String> params) throws IOException {
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
 
        httpConn.setDoInput(true); // true indicates the server returns response
 
        StringBuffer requestParams = new StringBuffer();
 
        if (params != null && params.size() > 0) {
 
            httpConn.setDoOutput(true); // true indicates POST request
 
            // creates the params string, encode them using URLEncoder
            Iterator<String> paramIterator = params.keySet().iterator();
            while (paramIterator.hasNext()) {
                String key = paramIterator.next();
                String value = params.get(key);
                requestParams.append(URLEncoder.encode(key, "UTF-8"));
                requestParams.append("=").append(
                        URLEncoder.encode(value, "UTF-8"));
                requestParams.append("&");
            }
 
            // sends POST data
            OutputStreamWriter writer = new OutputStreamWriter(
                    httpConn.getOutputStream());
            writer.write(requestParams.toString());
            writer.flush();
        }
 
        return httpConn;
    }
    
    public static HttpURLConnection sendPostFileRequest(String requestURL,
            File textFile) throws IOException {
        URL url = new URL(requestURL);
        String charset = "UTF-8";
        String param = "value";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
 
          httpConn = (HttpURLConnection) url.openConnection();
          httpConn.setDoOutput(true);
          httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

          httpConn.setDoInput(true); // true indicates the server returns response
 
        try (
        	    OutputStream output = httpConn.getOutputStream();
//        		OutputStream output = System.out;
        	    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
        	) {
        	    // Send normal param.
        	    writer.append("--" + boundary).append(CRLF);
        	    writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
        	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
        	    writer.append(CRLF).append(param).append(CRLF).flush();

        	    // Send text file.
        	    writer.append("--" + boundary).append(CRLF);
        	    writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
        	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
        	    writer.append(CRLF).flush();

                InputStream input = new FileInputStream(textFile.getAbsoluteFile());
                byte[] buffer = new byte[1024]; // Adjust if you want
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1)
                {
                    output.write(buffer, 0, bytesRead);
                }

        	    //copy(textFile.getAbsolutePath(), output);
        	    output.flush(); // Important before continuing with writer!
        	    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

        	    // End of multipart/form-data.
        	    writer.append("--" + boundary + "--").append(CRLF).flush();
        	}

        return httpConn;
    }
 
    /**
     * Returns only one line from the server's response. This method should be
     * used if the server returns only a single line of String.
     *
     * @return a String of the server's response
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static String readSingleLineRespone() throws IOException {
        InputStream inputStream = null;
        if (httpConn != null) {
            inputStream = httpConn.getInputStream();
        } else {
            throw new IOException("Connection is not established.");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
 
        String response = reader.readLine();
        reader.close();
 
        return response;
    }
 
    /**
     * Returns an array of lines from the server's response. This method should
     * be used if the server returns multiple lines of String.
     *
     * @return an array of Strings of the server's response
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static String[] readMultipleLinesRespone() throws IOException {
        InputStream inputStream = null;
        if (httpConn != null) {
            inputStream = httpConn.getInputStream();
        } else {
            throw new IOException("Connection is not established.");
        }
 
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        List<String> response = new ArrayList<String>();
 
        String line = "";
        while ((line = reader.readLine()) != null) {
            response.add(line);
        }
        reader.close();
 
        return (String[]) response.toArray(new String[0]);
    }
     
    /**
     * Closes the connection if opened
     */
    public static void disconnect() {
        if (httpConn != null) {
            httpConn.disconnect();
        }
    }

    /*
    protected String doInBackground(String... params) {

        //INSERT YOUR FUNCTION CALL HERE

        return "Executed!";
    }
    */
}

