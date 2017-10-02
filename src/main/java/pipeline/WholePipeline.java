package pipeline;

import com.github.mauricioaniche.ck.Runner;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static pipeline.FileHandler.*;
import static pipeline.Git.getNumDaysBtw2Dates;
import static pipeline.ResultFileWriter.log;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;

public class WholePipeline
{

    //TODO save output folder for the repository
    //TODO check slopes calculation


    static String OUTPUT_FOLDER_NAME;
    static String REPO_NAME;
    static File PATH_TO_REPOSITORY;
    static String REPOSITORY_HISTORY_FILE_PATH;
    static String COMMIT_IDS_FILE_PATH;
    static String LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT;
    static String LINES_FROM_CONSOLE_RUNNING_PMD;


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        if(args==null || args.length != 2)
        {
            log();
            System.out.println("Usage arguments: <repository url>");
            System.exit(1);
        }

        String repositoryURL = args[0];
        REPO_NAME = (args[1]);


        retrieveWholeRepoHistory(repositoryURL);



//        System.out.println(commitIdsWithFileSmells);


//        String resultFilePath = createEmptyFinalResultFile(repoName);
//
//        //generate one result line
//        //write that line to result file
//
//        int commitCount = 0;
//        for (String commitID : commitIds)
//        {
//            ArrayList<String> allFileNamesInCommit = getAllFileNamesInCommit(commitID);
//            System.out.println("ALL file names: " + allFileNamesInCommit);
//            for (String fileName : allFileNamesInCommit)
//            {
//                System.out.println("Handle file " + fileName);
//                handleFileInCommit(fileName, commitID, commitCount, commitIdsWithDates, commitIdsWithFileSmells, resultFilePath);
//            }
//            System.out.println("commitCount" + commitCount);
//            commitCount++;
//        }



    }

    private static void retrieveWholeRepoHistory(String repositoryURL) throws IOException, InterruptedException, ParseException {

        Git.clone_repository(repositoryURL);

        List<String> linesFromConsole = Git.retrieveCommitsForRepoAndSaveResultsToFile(); //runs git log in reverse order & saves whole history in output/all_commits.txt

        //REQUIREMENTS
        /*
        1) only java
        2) at least 1 year of history
        3) at least 50 java files
        4) 5k ELOC
        5) Popularity: at least 10 starts
         */

        LinkedHashMap<String, Calendar> commitIdsWithDates = FileHandler.getAllCommitIdsFromConsoleLines(linesFromConsole);
        ArrayList<String> commitIds = new ArrayList<String>(commitIdsWithDates.keySet());

//        if(!has1YearOfHistory(commitIdsWithDates))
//        {
//            System.err.println("EXIT because repository " + repositoryURL + " does not have 1 year of history.");
//            System.exit(5);
//        }

        System.out.println("Creating final result file for each commit");
        HashMap<String, HashMap<String,ArrayList<String>>> commitIdsWithFileSmells =  createOutputFileForEachCommit(commitIds);

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



    /*
        The flow. for each commit:
        1) checkout
        2) run pmd / get smells
        3) compute quality metrics
        4) produce one final file with all data
     */
    private static HashMap<String, HashMap<String,ArrayList<String>>> createOutputFileForEachCommit(ArrayList<String> commitIds) throws IOException
    {


        HashMap<String, HashMap<String,ArrayList<String>>> commitIdsWithFileSmells = new HashMap<>();

        for (String commit : commitIds)
//        String commit;
//        for (int i = 0; i < 5; i++)

        {
 //            System.out.print(commit);
//            commit = commitIds.get(i);


            String pathFinalCommitResultFile = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");
            File finalCommitFile = new File(pathFinalCommitResultFile);

            String pathToFileWithCommitSmells = OUTPUT_FOLDER_NAME.concat(commit).concat("-smells.csv");
            File finalCommitFileWithSmells = new File(pathToFileWithCommitSmells);

            //skip if both files exist
             //(!finalCommitFile.isFile() && !finalCommitFileWithSmells.isFile())
            {
                //checkout commit
                Git.executeCommandsAndReadLinesFromConsole(LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT, "/bin/bash", "-c", "cd " + PATH_TO_REPOSITORY + " && git checkout " + commit);

                //run pmd for each commit
                SmellDetector.runSmellDetector(pathToFileWithCommitSmells);
            }

                //add smells to hashmap
                HashMap<String, ArrayList<String>> fileNamesWithSmells = readSmellsFromFileToHashmap(pathToFileWithCommitSmells);

                Runner.computeQualityMetrics(String.valueOf(PATH_TO_REPOSITORY), pathFinalCommitResultFile, fileNamesWithSmells);

                commitIdsWithFileSmells.put(commit, fileNamesWithSmells);


            }
            System.out.println();

        return commitIdsWithFileSmells;

    }
}
