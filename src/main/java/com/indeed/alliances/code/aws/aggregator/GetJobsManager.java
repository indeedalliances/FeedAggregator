package com.indeed.alliances.code.aws.aggregator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * A thin wrapper around the ApiClient that handles serialization.
 * TODO If the ApiClient requires pagination, paging through the results
 * would be handled in a loop surrounding the try/catch block.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class GetJobsManager {

    /**
     * Get all jobs from the endpoint represented by the ApiConfig.
     * @param config
     * @throws Exception
     */
    public static void getJobs(ApiConfig config) throws Exception {
        // create tempfile
        String tempFile = config.xml_output_directory + "/" + config.xml_output_filename;
        File f = new File(tempFile);
        f.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        boolean done = false;
        try {
            // get the jobs... returns 1 if we got all jobs, else the page at which it stopped
            String jobs = ApiClient.getJobs(config);
            writer.write(jobs.toCharArray());
            // cleanup
            writer.flush();
            writer.close();
        } catch (Exception e) {
            // cleanup
            writer.flush();
            writer.close();
            throw new Exception("Exception thrown getting jobs for "+config.name+" original exception message was: "+e.getMessage());
        }
    }
}
