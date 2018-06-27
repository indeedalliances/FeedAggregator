package com.indeed.alliances.code.aws.aggregator;


/**
 * A class that represents configuration parameters that are required in order to
 * access an API endpoint.
 *
 * In this case, there is no authentication information, page size, etc.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class ApiConfig {

    /**
     * A unique identifier for this endpoint. It will be referenced in error messages.
     */
    String name;
    /**
     * The URL of this endpoint.
     */
    String url;
    /**
     * The directory where the XML file containing data for this endpoint will be written.
     */
    String xml_output_directory;
    /**
     * The name that will be used for the XML file containing data for this endpoint.
     */
    String xml_output_filename;
    /**
     * A comma delimited list of email addresses to whom error messages should be sent.
     */
    String error_email_list;
    /**
     * The "from" email address to be used for error messages.
     */
    String error_email_from_address;

    public ApiConfig() { }

}
