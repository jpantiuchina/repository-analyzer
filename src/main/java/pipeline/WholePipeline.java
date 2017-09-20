package pipeline;

import com.github.mauricioaniche.ck.Runner;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static pipeline.ResultFileWriter.log;
import static pipeline.ResultFileWriter.OUTPUT_FOLDER_NAME;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;

public class WholePipeline
{
    //public static HashMap<String,String> smellTypes = new HashMap();


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        if(args==null || args.length != 2)
        {
            log();
            System.out.println("Usage arguments: <repository url>");
            System.exit(1);
        }

//        BasicConfigurator.configure();

        String repositoryURL = args[0];
        String clonedRepoFolderName = (args[1]);

        String PATH_TO_REPOSITORY = Git.clone_repository(repositoryURL, clonedRepoFolderName);

        String REPOSITORY_HISTORY_FILE_PATH = OUTPUT_FOLDER_NAME + "linesFromConsole.txt";
        List<String> linesFromConsole = Git.retrieveCommitsForRepoAndSaveResultsToFile(PATH_TO_REPOSITORY, REPOSITORY_HISTORY_FILE_PATH); //runs git log in reverse order & saves whole history in output/all_commits.txt

        ArrayList<String> commitIds = IntervalSplitter.getAllCommitIdsFromConsoleLines(linesFromConsole);

        String COMMIT_IDS_FILE_PATH = OUTPUT_FOLDER_NAME + "sorted_commit_Ids.txt";

        IntervalSplitter.saveCommitIdsToFile(commitIds, COMMIT_IDS_FILE_PATH); // prints to console & saves grouped commits to result.txt


        System.out.println("Creating final result file for each commit");
        createOutputFileForEachCommit(PATH_TO_REPOSITORY, commitIds);


        
        System.out.println("GOOD");
        System.exit(10);
        //createEmptyFinalResultFile();

        //handleGroups(groups);

    }

    /*
        The flow. for each commit:
        1) checkout
        2) run pmd / get smells
        3) compute quality metrics
        4) produce one final file with all data
     */
    //TODO. doing here: computeQualityMetrics
    private static void createOutputFileForEachCommit(String PATH_TO_REPOSITORY, ArrayList<String> commitIds) throws IOException
    {

        String linesFromConsoleCheckoutCommits = OUTPUT_FOLDER_NAME + "linesFromConsoleCheckoutCommits.txt";

        for (String commit : commitIds)
//        String commit;
//        for (int i = 0; i < 5; i++)

        {
 //            System.out.print(commit);
//            commit = commitIds.get(i);

                //checkout each commit of the whole repository
                Git.executeCommandsAndReadLinesFromConsole(linesFromConsoleCheckoutCommits,"/bin/bash", "-c", "cd " + PATH_TO_REPOSITORY + " && git checkout " + commit);
                String commitFileName = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");

                //run pmd for each commit
                String pathToFileWithCommitSmells = OUTPUT_FOLDER_NAME.concat(commit).concat("-smells.csv");
                SmellDetector.runSmellDetector(PATH_TO_REPOSITORY,pathToFileWithCommitSmells);

                //add smells to hashmap
                HashMap<String, ArrayList<String>> fileNamesWithSmells = readSmellsFromFileToHashmap(pathToFileWithCommitSmells);

                String pathToQualityMetricsResultFile = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");
                Runner.computeQualityMetrics(PATH_TO_REPOSITORY, pathToQualityMetricsResultFile, fileNamesWithSmells);
            }
            System.out.println();




    }


}
