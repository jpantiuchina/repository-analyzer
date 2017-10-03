package pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pipeline.WholePipeline.*;
import static pipeline.WholePipeline.LINES_FROM_CONSOLE_RUNNING_PMD;
import static pipeline.WholePipeline.PATH_TO_REPOSITORY;


class Util
{

    private final static String TAG = Git.class.getCanonicalName();

    /*
    Equivalent to: PrintStream out = new PrintStream(new FileOutputStream(pathToFile, true))
try { out.println(lineToAdd); } finally { out.close(); }
try-with-resources
     */
    static void writeLineToFile(String lineToAdd, String pathToFile) throws IOException
    {
        try (PrintStream out = new PrintStream(new FileOutputStream(pathToFile, true)))
        {
            out.println(lineToAdd);
        }
    }






    static void log()
    {   //[2] - gets the previous method, hence, the one from which log() i called
        System.err.println("LOG: " + TAG + "." + Thread.currentThread().getStackTrace()[2].getMethodName());
    }


    static void createPaths()
    {
        OUTPUT_FOLDER_NAME = "history/output-" + REPO_NAME + "/";

        PATH_TO_REPOSITORY = new File(OUTPUT_FOLDER_NAME + REPO_NAME);

        REPOSITORY_HISTORY_FILE_PATH = PATH_TO_REPOSITORY + "/linesFromConsole.txt";
        COMMIT_IDS_FILE_PATH = PATH_TO_REPOSITORY + "/sorted_commit_Ids.txt";
        LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT = PATH_TO_REPOSITORY + "/linesFromConsoleOnCommitsCheckout.txt";
        LINES_FROM_CONSOLE_RUNNING_PMD = PATH_TO_REPOSITORY + "/LinesFromConsoleRunningPmd.txt";
    }


    static String getResultFileNameFromRepositoryURL(String url)
    {
        Util.log();

        final Pattern pattern = Pattern.compile("([^\\/]+)$");
        final Matcher matcher = pattern.matcher(url);

        String filename = null;
        if (matcher.find())
        {
            filename = matcher.group(0);
        }
        else
        {
            System.err.println("Impossible to generate result filename from repository");
            System.exit(4);
        }


        return (filename);
    }


}
