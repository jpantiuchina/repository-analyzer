package pipeline;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static pipeline.FileHandler.*;
import static pipeline.Git.getAllCommitIdsAndCreateFileWithSortedIds;
import static pipeline.Git.sortCommits;
import static pipeline.Util.*;


public class WholePipeline
{

    static ArrayList<String> CSV_FILE_NAMES = new ArrayList<>();
    //static ArrayList<String> CLEAN_CSV_FILE_NAMES = new ArrayList<>();

    static String REPO_FOLDER_NAME;
    static String REPOSITORY_HISTORY_FILE_PATH;
    static String COMMIT_IDS_FILE_PATH;

    private static ArrayList<String> COMMIT_INDEX_TO_COMMIT_ID = new ArrayList<>();
    static Map<String, Integer> COMMIT_ID_TO_COMMIT_INDEX = new HashMap<>();

    static LinkedHashMap<String, Calendar> COMMIT_IDS_WITH_DATES = new LinkedHashMap<>();

    static String PATH_TO_SMELLY_FINAL_RESULT_FILE;
    static String PATH_TO_CLEAN_FINAL_RESULT_FILE;
    static String PATH_TO_OTHER_SMELLY_FINAL_RESULT_FILE;

    static ArrayList<String> uniquePairs = new ArrayList<>();

    static int MAX_INT_DAYS_OR_COMMITS = 120;
    static boolean IS_PREDICTION_IN_DAYS = true;
    static int STEP = 15;


    static ArrayList<FileData> allFilesData = new ArrayList<>();


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        System.out.println("");
        System.out.println("----------------------------------------------------------------");

        if (args.length != 4)
        {
            Util.log();
            System.out.println("Usage argument: <repositoryFolderPath> <isPredictInDays> <maxIntervalLength> <Step>");
            System.exit(1);
        }

        String pathToRepositoryFolderWithAllData = args[0].trim();

        IS_PREDICTION_IN_DAYS = parseBoolean(args[1].trim());

        MAX_INT_DAYS_OR_COMMITS = parseInt(args[2].trim());

        STEP = parseInt(args[3].trim());

        createPaths(pathToRepositoryFolderWithAllData);

        getAllCommitIdsAndCreateFileWithSortedIds();


        File[] allFilesInFolder = getAllFilesInFolder(REPO_FOLDER_NAME.concat("/bad_smell"));

        handleAllFilesForRepo(allFilesInFolder);

        sortCommits();


        removeFileIfPresent(COMMIT_IDS_FILE_PATH);

        for (String commit : COMMIT_IDS_WITH_DATES.keySet())
        {
            writeLineToFile(commit, COMMIT_IDS_FILE_PATH);
            COMMIT_INDEX_TO_COMMIT_ID.add(commit);
            COMMIT_ID_TO_COMMIT_INDEX.put(commit, COMMIT_INDEX_TO_COMMIT_ID.size() - 1);
//            System.out.println(commit + " => " + (COMMIT_INDEX_TO_COMMIT_ID.size() - 1));
        }


        System.out.println("#Commits in repository: " + COMMIT_INDEX_TO_COMMIT_ID.size());
        System.out.println("Processing commits...");

        handleSmellyFiles();
        addCleanFilesForEverySmellyFileInCommit();

        removeEmptyFiles();

        System.out.println("----------------------------------------------------------------");


    }
}
