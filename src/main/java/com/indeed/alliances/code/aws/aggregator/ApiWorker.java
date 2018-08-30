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
    private Boolean checkHealth;

    public ApiWorker(ApiConfig config, Map<ApiConfig,Exception> exceptionMap, Boolean checkHealth) {
        this.config = config;
        this.exceptionMap = exceptionMap;
        this.checkHealth = checkHealth;
    }

    @Override
    public void run() {
        try {
            if(checkHealth) {
                ApiClient.checkHealth(this.config);
            } else {
                GetJobsManager.getJobs(this.config);
            }
        } catch (Exception e) {
            // add the exception to the map
            synchronized(exceptionMap) {
                exceptionMap.put(this.config, e);
            }
        }
    }
}
