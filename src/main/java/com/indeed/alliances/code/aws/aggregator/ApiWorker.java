package com.indeed.alliances.code.aws.aggregator;

import java.util.Map;

/**
 * A runnable class for multi=threading the data extraction
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * August 2018
 */
public class ApiWorker implements Runnable {

    private ApiConfig config;
    private Map<ApiConfig,Exception> exceptionMap;

    public ApiWorker(ApiConfig config, Map<ApiConfig,Exception> exceptionMap) {
        this.config = config;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public void run() {
        try {
            GetJobsManager.getJobs(this.config);
        } catch (Exception e) {
            // add the exception to the map
            synchronized(exceptionMap) {
                exceptionMap.put(this.config, e);
            }
        }
    }
}
