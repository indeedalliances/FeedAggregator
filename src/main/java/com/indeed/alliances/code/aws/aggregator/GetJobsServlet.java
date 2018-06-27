package com.indeed.alliances.code.aws.aggregator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import com.google.gson.Gson;
import org.apache.commons.io.comparator.NameFileComparator;

/**
 * Uses environment variable "preserve_file_count"
 * which is the number of files in the XML output directory to preserve
 * i.e., it it's set to 20, and there are 21 files, the oldest file will be deleted.
 * The default is 20.
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */
public class GetJobsServlet extends HttpServlet {

    private volatile static Integer sem = 0;

    public GetJobsServlet() {}

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // ensure that we can't run more than one at a time
        synchronized(sem) {
            // mark start
            Long start = System.currentTimeMillis();
            // read api configs from json
            File apiFile = new File("api_configs.json");
            FileReader fr = new FileReader(apiFile);
            Gson gson = new Gson();
            ApiConfig[] configs = gson.fromJson(fr, ApiConfig[].class);
            // get jobs for each config
            for (int i = 0; i < configs.length; i++) {
                ApiConfig getConfig = null;
                try {
                    getConfig = configs[i];
                    // get the jobs
                    GetJobsManager.getJobs(getConfig);
                } catch (Exception e) {
                    // send notification email
                    String message = "\n\nError retrieving jobs:\n\n" + e.getMessage();
                    Utils.sendEmails(getConfig.error_email_from_address,
                            getConfig.error_email_list,
                            "GetJobs error for " + getConfig.name,
                            message);
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().println(e.getMessage());
                }
            }
            // assemble temp files in to 1, adding header and footer
            String masterFileString = Constants.XML_OUTPUT_DIRECTORY + "/" + Constants.JOBS_FILE_NAME;
            ApiConfig assembleConfig = null; // declare here for visibility during error processing
            OutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(masterFileString), false);
                fos.write("<?xml version=\"1.0\" encoding=\"utf-8\"?><JobPostings>".getBytes());
                for (int i = 0; i < configs.length; i++) {
                    assembleConfig = configs[i];
                    InputStream in = new FileInputStream(assembleConfig.xml_output_directory + "/" + assembleConfig.xml_output_filename);
                    byte[] buffer = new byte[1 << 20];  // loads 1 MB of the file
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                    fos.flush();
                    close(in);
                }
                fos.write("</JobPostings>".getBytes());
            } catch (Exception e) {
                Utils.sendEmails(assembleConfig.error_email_from_address,
                        assembleConfig.error_email_list,
                        "Assemble error for " + assembleConfig.name,
                        e.getMessage());
            } finally {
                fos.flush();
                close(fos);
            }

            // pretty format the jobs
            String tempFile = Constants.XML_OUTPUT_DIRECTORY + "/" + Constants.TEMP_JOB_FILE_NAME;
            ProcessBuilder builder = new ProcessBuilder("python", "prettyprint.py", masterFileString, "utf-8");
            builder.redirectOutput(new File(tempFile));
            builder.redirectError(new File("errors.txt"));
            builder.start(); // may throw IOException
            // re-name the XML file to include timestamp
            String toFile = Constants.XML_OUTPUT_DIRECTORY + "/" + System.currentTimeMillis() + "_" + Constants.JOBS_FILE_NAME;
            Path path = Files.move(Paths.get(tempFile), Paths.get(toFile));
            if (path == null) {
                throw new ServletException("unable to copy time-stamped file");
            }
            // clear old files
            File dir = new File(Constants.XML_OUTPUT_DIRECTORY);
            File files[] = dir.listFiles();
            Arrays.sort(files, NameFileComparator.NAME_INSENSITIVE_REVERSE);
            int preservFileCount = 20;
            String pfcString = System.getenv("preserve_file_count");
            if (pfcString != null)
                preservFileCount = Integer.parseInt(pfcString);

            for (int i = files.length - 1; i > (preservFileCount - 1); i--) {
                File f = files[i];
                try {
                    // insurance that we don't delete latest file for some reason
                    if (!f.getAbsolutePath().equals(toFile) && !f.getAbsolutePath().equals(masterFileString))
                        f.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //
            System.out.println("getJobs() took " + (System.currentTimeMillis() - start) + "ms");
            // acknowledge to caller
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Success!</h1>");
        }
    }

    private void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}