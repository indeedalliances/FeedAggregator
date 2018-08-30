package com.indeed.alliances.code.aws.aggregator;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        // initialize the exception map, for holding exceptions when getting feeds
        Map<ApiConfig, Exception> exceptionMap = new HashMap<>();
        // initialize threadpool
        int tpc = 4;
        try {
            tpc = Integer.parseInt(System.getenv("threadpool_count"));
        } catch (Exception e) {
            // dud
        }
        ExecutorService executor = Executors.newFixedThreadPool(tpc);
        // ensure all endpoints are reachable
        ApiConfig config = null;
        // get jobs for each config
        for (int i = 0; i < configs.length; i++) {
            ApiConfig getConfig = configs[i];
            // check endpoint health
            Runnable worker = new ApiWorker(getConfig,exceptionMap, true);
            executor.execute(worker);
        }
        // shutdown threadpool
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        // send notification email, if exceptions occurred
        if(exceptionMap.size()>0) {
            Iterator<ApiConfig> it = exceptionMap.keySet().iterator();
            while(it.hasNext()) {
                ApiConfig c = it.next();
                Exception e = exceptionMap.get(c);
                String message = "\n\nError retrieving jobs during HealthCheck:\n\n" + e.getMessage();
                Utils.sendEmails(c.error_email_from_address,
                        c.error_email_list,
                        "HealthCheck error for " + c.name,
                        message);
            }
        }
        // send response
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>I am Healthy</h1>");
    }
}