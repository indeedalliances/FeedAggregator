package com.indeed.alliances.code.aws.aggregator;

/**
 * A class containing constants to be used through the system.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class Constants {

    /**
     * The name of the temporary file that will be used as output from the prettyprint utility
     * after the data is aggregated, but before it takes it's final name.
     */
    public static final String TEMP_JOB_FILE_NAME = "temp.xml";

    /**
     * The name of the file to be used for the aggregation of the data from all the endpoints.
     * The same filename will be used for the final output, where the filename will be appeneded
     * to the system time in millis.
     */
    public static final String JOBS_FILE_NAME = "jobs.xml";

    /**
     * the output directory where the final file should be stored.
     */
    public static final String XML_OUTPUT_DIRECTORY = "/mnt/efs";

}
