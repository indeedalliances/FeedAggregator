package com.indeed.alliances.code.aws.aggregator;

import org.apache.commons.io.FileUtils;

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

    public static void getJobs(ApiConfig config) throws Exception {
        BufferedWriter writer = null;
        try {
            // create tempfile
            String outFile = config.xml_output_directory + "/" + config.xml_output_filename;
            String tmpFile = outFile + ".tmp";
            File f = new File(tmpFile);
            f.createNewFile();
            writer = new BufferedWriter(new FileWriter(f));
            boolean done = false;
            while (!done) {
                try {
                    // get the jobs... returns true if we got all jobs
                    // the config object should have all the fields required
                    // to walk through all the calls required to get all the jobs
                    done = ApiClient.getJobs(config, writer);
                } catch (Exception e) {
                    // cleanup
                    writer.flush();
                    writer.close();
                    throw new Exception("Exception thrown getting jobs for " + config.name + " original exception message was: " + e.getMessage());
                }
            }
            writer.flush();
            writer.close();
            // delete old file
            File old = new File(outFile);
            if(old.exists()) {
                old.delete();
            }
            // move tmp file to outfile
            FileUtils.moveFile(FileUtils.getFile(tmpFile),FileUtils.getFile(outFile));
        } finally {
            // cleanup tmp file in case of exception
            try {
                writer.flush();
                writer.close();
            } catch(Exception e) {}
        }
    }
}
