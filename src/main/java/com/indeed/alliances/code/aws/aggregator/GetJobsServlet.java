package com.indeed.alliances.code.aws.aggregator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

/**
 * Uses environment variable "preserve_file_count"
 * which is the number of files in the XML output directory to preserve
 * i.e., it it's set to 20, and there are 21 files, the oldest file will be deleted.
 * The default is 20.
 *
 * Uses environment variable "max_workingfile_age" which is the max age, in minutes, that the
 * working file from a given API endpoint is considered viable. This comes in to play when
 * any given API endpoint fails for any reason. If there exists a previous working file for
 * that endpoint, and it is younger than max_workingfile_age, it will still be aggregated in
 * to the master XML file. The default is 70 minutes.
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
            // get jobs for each config
            for (int i = 0; i < configs.length; i++) {
                ApiConfig getConfig = null;
                getConfig = configs[i];
                // get the jobs
                Runnable worker = new ApiWorker(getConfig,exceptionMap, false);
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
                    String message = "\n\nError retrieving jobs:\n\n" + e.getMessage();
                    Utils.sendEmails(c.error_email_from_address,
                            c.error_email_list,
                            "GetJobs error for " + c.name,
                            message);
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
                    // only use the working file if it's young enough
                    if(checkFileDate(assembleConfig)) {
                        InputStream in = new FileInputStream(assembleConfig.xml_output_directory + "/" + assembleConfig.xml_output_filename);
                        byte[] buffer = new byte[1 << 20];  // loads 1 MB of the file
                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                        fos.flush();
                        close(in);
                    } else {
                        // set the working file length to 0 because it is too old to be used
                        String workingFile = assembleConfig.xml_output_directory + "/" + assembleConfig.xml_output_filename;
                        File f = new File(workingFile);
                        FileWriter fw = new FileWriter(f);
                        fw.flush();
                        fw.close();
                    }
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
            // sort file list
            File dir = new File(Constants.XML_OUTPUT_DIRECTORY);
            File files[] = dir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File o1, File o2) {
                if ((o1).lastModified() > (o2).lastModified()) {
                    return -1;
                } else if ((o1).lastModified() < (o2).lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
                }
            });
            int preserveFileCount = 20;
            String pfcString = System.getenv("preserve_file_count");
            if (pfcString != null)
                preserveFileCount = Integer.parseInt(pfcString);
            // make sure we keep the latest working files as well as pre-DT-stamped file
            preserveFileCount += configs.length + 1;
            // clear old files
            for (int i = files.length - 1; i > (preserveFileCount - 1); i--) {
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
            Long duration = System.currentTimeMillis() - start;
            System.out.println("getJobs() took " + duration + "ms");
            System.out.println("using "+tpc+" download threads");
            // acknowledge to caller
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("getJobs() took " + duration + "ms");
            response.getWriter().println("using "+tpc+" download threads");
            if(exceptionMap.size() > 0) {
                response.getWriter().println("<h1>Errors!</h1>");
                Iterator<ApiConfig> it = exceptionMap.keySet().iterator();
                while(it.hasNext()) {
                    ApiConfig c = it.next();
                    Exception e = exceptionMap.get(c);
                    String message = "Error retrieving jobs for "+c.name+": " + e.getMessage()+"<br>";
                    response.getWriter().println(message);
                }
            } else {
                response.getWriter().println("<h1>Success!</h1>");
            }
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

    private static Long max_age = null;
    private boolean checkFileDate(ApiConfig gjConfig) {
        if(max_age==null) {
            try {
                String s = System.getenv("max_workingfile_age"); // age in minutes
                max_age = Long.parseLong(s);
                max_age = max_age * 60000; // translate mins to millis
            } catch(Exception e) {
                max_age = 4200000L; // 70 mins in millis
            }
        }
        File f = FileUtils.getFile(gjConfig.xml_output_directory + "/" + gjConfig.xml_output_filename);
        if(System.currentTimeMillis()-f.lastModified()>max_age) {
            return false;
        }
        return true;
    }
}