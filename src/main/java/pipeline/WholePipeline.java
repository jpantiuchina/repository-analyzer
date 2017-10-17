package pipeline;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import static pipeline.FileHandler.createFinalFileDataLine;
import static pipeline.FileHandler.handleFilesInCommits;
import static pipeline.Git.getNumDaysBtw2Dates;
import static pipeline.Git.readDateFromLine;
import static pipeline.Git.retrieveWholeRepoHistory;
import static pipeline.ResultFileWriter.log;
import static pipeline.Util.createPaths;
import static pipeline.Util.getResultFileNameFromRepositoryURL;


//TODO createOutputFileForEachCommit not redirected console output to file on commit checkout
// TODO PMD console output is not redirected to file
// TODO executeCommandsAndReadLinesFromConsole disabled exception handling, because PMD throws exception with code -1


//TODO save output folder for the repository
//TODO check slopes calculation
//TODO
    /*
            REQUIREMENTS

        1) only java
        2) at least 1 year of history
        3) at least 50 java files
        4) 5k ELOC
        5) Popularity: at least 10 starts
         */

public class WholePipeline
{

    static String OUTPUT_FOLDER_NAME;
    static String REPO_NAME;
    public static File PATH_TO_REPOSITORY;
    static String REPOSITORY_HISTORY_FILE_PATH;
    static String COMMIT_IDS_FILE_PATH;
    static String FILE_PATH_TO_LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT; //not produced TODO
    static String FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD; //not produced TODO

    static String PATH_TO_SMELLY_FINAL_RESULT_FILE;
    static String PATH_TO_CLEAN_FINAL_RESULT_FILE;


    static ArrayList<String> COMMIT_IDS = new ArrayList<>();
    static LinkedHashMap<String, Calendar> COMMIT_IDS_WITH_DATES = new LinkedHashMap<>();
    static ArrayList<String> SMELLY_FILE_NAMES = new ArrayList<>();


    public static int DAYS;


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        System.err.println("");
        System.err.println("----------------------------------------------------------------");
        Util.log();


        if (args.length != 1)
        {
            log();
            System.out.println("Usage arguments: <repository url>");
            //System.out.println("Usage arguments: <repository url> <time interval in days> <# of clean classes>");
          //  System.out.println("Wrong 3rd [--keep-history] argument");
            System.exit(1);
        }

        String repositoryURL = args[0].trim();
        //DAYS = Integer.parseInt(args[1]);
       // System.out.println("URL: " + repositoryURL);
        REPO_NAME = getResultFileNameFromRepositoryURL(repositoryURL);

        createPaths();

        //Comment if history was already created
        retrieveWholeRepoHistory(repositoryURL);
        System.out.println("History files were successfully created for repository: " + REPO_NAME);

        //         OUTPUT_FOLDER_NAME contains result files for every commit
       // COMMIT_IDS_FILE_PATH  contains commit ids sorted
        // REPOSITORY_HISTORY_FILE_PATH (git log with dates)


//        String line = createFinalFileDataLine("/Users/jevgenijapantiuchina/Desktop/smell-prediction/repository-analyzer/history/output-java/java/src/main/java/com/jsoniter/output/CodegenImplNative.java","b40fd4ff266525bd3113520bea2ca595781dfb28",true);
//        System.out.println(line);

        String line = createFinalFileDataLine("/Users/jevgenijapantiuchina/Desktop/smell-prediction/repository-analyzer/history/output-java/java/src/test/java/com/jsoniter/TestNull.java", "1836034d76aa9858f8531f061b076a59e13a81ea", false);
       System.out.println(line);


            //fails d79fc1e6cec5ec702565e59625227d207488cb61
       //handleFilesInCommits();


    }




}
