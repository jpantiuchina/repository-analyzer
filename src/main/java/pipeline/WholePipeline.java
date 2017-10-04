package pipeline;

import com.github.mauricioaniche.ck.Runner;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static pipeline.Git.retrieveWholeRepoHistory;
import static pipeline.ResultFileWriter.log;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;
import static pipeline.Util.createPaths;
import static pipeline.Util.getResultFileNameFromRepositoryURL;
import static pipeline.Util.writeLineToFile;


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
    static String FILE_PATH_TO_LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT;
    static String FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD;

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
        System.out.println("URL: " + repositoryURL);
        REPO_NAME = getResultFileNameFromRepositoryURL(repositoryURL);

        createPaths();

        //Comment if history was already created
        retrieveWholeRepoHistory(repositoryURL);
        System.out.println("History files were successfully created for repository: " + REPO_NAME);

    }



    static void createOutputFileForEachCommit(ArrayList<String> commitIds) throws IOException, InterruptedException
    {
        ArrayList<String> linesFromConsoleOnCommitCheckout = new ArrayList<>();

        for (String commit : commitIds)
        {
            String pathToFileWithCommitSmells = OUTPUT_FOLDER_NAME.concat(commit).concat("-smells.csv");
            String pathFinalCommitResultFile = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");

            File finalCommitFile = new File(pathFinalCommitResultFile);
            File finalCommitFileWithSmells = new File(pathToFileWithCommitSmells);

            if (!finalCommitFile.exists() && !finalCommitFileWithSmells.exists())
            {
                //checkout each commit of the whole repository
                linesFromConsoleOnCommitCheckout.addAll(Git.executeCommandsAndReadLinesFromConsole(
                        PATH_TO_REPOSITORY, "git", "checkout", "-f", commit));

                //run pmd for each commit
                SmellDetector.runSmellDetector(pathToFileWithCommitSmells);
            }


            HashMap<String, ArrayList<String>> fileNamesWithSmells = readSmellsFromFileToHashmap(pathToFileWithCommitSmells);

            Runner.computeQualityMetricsAndSmellsForCommitAndSaveToFile(pathFinalCommitResultFile, fileNamesWithSmells);

            }

        for (String line: linesFromConsoleOnCommitCheckout)
        {
            writeLineToFile(line, FILE_PATH_TO_LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT);
        }
    }
}
