package pipeline;

import com.github.mauricioaniche.ck.Runner;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static pipeline.Git.getNumDaysBtw2Dates;
import static pipeline.Git.retrieveWholeRepoHistory;
import static pipeline.ResultFileWriter.log;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;
import static pipeline.Util.createPaths;
import static pipeline.Util.getResultFileNameFromRepositoryURL;


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
    static String LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT;
    static String LINES_FROM_CONSOLE_RUNNING_PMD;

    public static int DAYS;


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {

        System.err.println("");
        System.err.println("----------------------------------------------------------------");
        Util.log();

        if (args.length != 2)
        {
            log();
            System.out.println("Usage arguments: <repository url> <time interval in days> <# of clean classes>");
            System.out.println("Wrong 3rd [--keep-history] argument");
            System.exit(1);
        }

        String repositoryURL = args[0].trim();
        //DAYS = Integer.parseInt(args[1]);
        System.out.println("URL: " + repositoryURL);
        REPO_NAME = getResultFileNameFromRepositoryURL(repositoryURL);

        createPaths();

        if (!PATH_TO_REPOSITORY.exists())
        {
           Git.cloneRepository(repositoryURL);

        }


        //Comment if history was already created
        retrieveWholeRepoHistory(repositoryURL);

        System.out.println("History files were successfully created for repository: " + REPO_NAME);

    }





    private static boolean has1YearOfHistory(LinkedHashMap<String, Calendar> commitIdsWithDates)
    {
        String firstID = commitIdsWithDates.keySet().iterator().next();
        Calendar firstCommitDate = commitIdsWithDates.get(firstID);
        ArrayList<String> commitIds = new ArrayList<String>(commitIdsWithDates.keySet());
        String lastCommitID = commitIds.get(commitIdsWithDates.size()-1);
        Calendar lastCommitDate = commitIdsWithDates.get(lastCommitID);
       // System.out.println("1st commit date: " + firstCommitDate.getTime() + ", last commit date: " + lastCommitDate.getTime() + ", diff: " + getNumDaysBtw2Dates(firstCommitDate, lastCommitDate) );
        return getNumDaysBtw2Dates(firstCommitDate, lastCommitDate) >= 364;
    }



    static HashMap<String, HashMap<String,ArrayList<String>>> createOutputFileForEachCommit(ArrayList<String> commitIds) throws IOException, InterruptedException {


        HashMap<String, HashMap<String,ArrayList<String>>> commitIdsWithFileSmells = new HashMap<>();

        for (String commit : commitIds)

        {
            String pathFinalCommitResultFile = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");
            File finalCommitFile = new File(pathFinalCommitResultFile);

            if (!finalCommitFile.exists())
            {
                //checkout each commit of the whole repository
                Git.executeCommandsAndReadLinesFromConsole(
                        PATH_TO_REPOSITORY, "git", "checkout", "-f", commit);
            }


            String pathToFileWithCommitSmells = OUTPUT_FOLDER_NAME.concat(commit).concat("-smells.csv");
            File finalCommitFileWithSmells = new File(pathToFileWithCommitSmells);

            //check if file with smells is present, don't runt smell detector
            if (!finalCommitFileWithSmells.exists())
            {
                //run pmd for each commit
                SmellDetector.runSmellDetector(pathToFileWithCommitSmells);

            }

                //add smells to hashmap
                HashMap<String, ArrayList<String>> fileNamesWithSmells = readSmellsFromFileToHashmap(pathToFileWithCommitSmells);

                Runner.computeQualityMetricsAndSmellsForCommitAndSaveToFile(pathFinalCommitResultFile, fileNamesWithSmells);

                commitIdsWithFileSmells.put(commit, fileNamesWithSmells);


            }
            System.out.println();

        return commitIdsWithFileSmells;

    }
}
