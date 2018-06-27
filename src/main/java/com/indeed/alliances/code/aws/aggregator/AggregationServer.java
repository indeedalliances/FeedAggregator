package com.indeed.alliances.code.aws.aggregator;

import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * A light-weight servlet container based on Jetty, listening on port 8080.
 *
 * It has two endpoints:
 * HealthCheck, which pings all the API endpoints for connectivity, and
 * GetJobs, which kicks off the process of collecting all the jobs data
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class AggregationServer extends HttpServlet {

    public static void main( String[] args ) throws Exception {
        // create the server
        Server server = new Server(8080);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        // add health check servlet
        handler.addServletWithMapping(HealthCheckServlet.class, "/HealthCheck");
        // add get jobs servlet
        handler.addServletWithMapping(GetJobsServlet.class, "/GetJobs");
        // start the server
        server.start();
        server.join();
    }

}
