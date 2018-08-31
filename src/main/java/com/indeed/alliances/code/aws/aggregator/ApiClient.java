package com.indeed.alliances.code.aws.aggregator;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * A class representing a client that talks to an API. In this implementation,
 * there is no authentication or paging.
 *
 * It is up to the ATS to modify this class to work with their API.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class ApiClient {

    /**
     * Returns an XML string containing jobs data from the given endpoint
     * represented by the ApiConfig.
     *
     * @param apiConfig
     * @return true if all jobs were retrieved
     * @throws Exception
     */
    public static boolean getJobs(ApiConfig apiConfig, Writer writer) throws Exception {
        URL url = new URL(apiConfig.url);
        // create connection
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("accept", "text/xml");
        // get response
        urlConnection.connect();
        if (urlConnection.getResponseCode() != 200) {
            throw new Exception("Failed : HTTP error code : "
                    + urlConnection.getResponseCode());
        }
        BufferedReader responseBuffer = new BufferedReader(new InputStreamReader((urlConnection.getInputStream())));
        StringBuilder builder = new StringBuilder();
        String buff;
        while ((buff = responseBuffer.readLine()) != null) {
            builder.append(buff);
        }
        urlConnection.disconnect();
        String result = builder.toString();
        // TODO if the String result need to be cleaned, i.e. header/footer removal etc.
        // it would be done here before writing to buffer
        writer.write(result.toCharArray());
        return true;
    }

    /**
     * Performs a health check on the API endpoint. In this implementation, the URL
     * is pinged, but the enture response is not consumed; only the response code is
     * retrieved. Any response code other than 200 is considered a failure.
     *
     * @param apiConfig
     * @throws Exception
     */
    public static void checkHealth(ApiConfig apiConfig) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiConfig.url).openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HealthCheck failed for "+apiConfig.name+" "+apiConfig.url+" response code was "+responseCode);
        }
    }

}
