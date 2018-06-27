package com.indeed.alliances.code.aws.aggregator;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A health Check handler that pings all of the endpoints.
 * It will email an error message in the case of any endpoint failure.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2015
 */
public class HealthCheckServlet extends HttpServlet {

    public HealthCheckServlet() {}

    @Override
    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws ServletException, IOException {

        // read api configs from json
        File apiFile = new File("api_configs.json");
        FileReader fr = new FileReader(apiFile);
        Gson gson = new Gson();
        ApiConfig[] configs = gson.fromJson(fr, ApiConfig[].class);
        // ensure all endpoints are reachable
        int i=0;
        ApiConfig config = null;
        for(i =0; i<configs.length; i++) {
            config = configs[i];
            try {
                // ping API
                ApiClient.checkHealth(config);
            } catch(Exception e) {
                // send notification email
                String message ="\n\nError retrieving jobs during HealthCheck:\n\n"+e.getMessage();
                Utils.sendEmails(config.error_email_from_address,
                        config.error_email_list,
                        "HealthCheck error for "+config.name,
                        message);
            }
        }
        // send response
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>I am Healthy</h1>");

    }
}