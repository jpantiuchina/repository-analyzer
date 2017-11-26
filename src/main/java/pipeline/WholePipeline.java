package pipeline;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static pipeline.FileHandler.*;
import static pipeline.Git.getAllCommitIdsAndCreateFileWithSortedIds;
import static pipeline.Util.createPaths;
import static pipeline.Util.isFileEmpty;


public class WholePipeline
{

    static ArrayList<String> SMELLY_CSV_FILE_NAMES = new ArrayList<>();

    static String REPO_FOLDER_NAME;
    static String REPOSITORY_HISTORY_FILE_PATH;
    static String COMMIT_IDS_FILE_PATH;

    static ArrayList<String> COMMIT_IDS = new ArrayList<>();
    static LinkedHashMap<String, Calendar> COMMIT_IDS_WITH_DATES = new LinkedHashMap<>();

    static String PATH_TO_SMELLY_FINAL_RESULT_FILE;
    static String PATH_TO_CLEAN_FINAL_RESULT_FILE;

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

        handleAllFiles(allFilesInFolder);

        addCommitFilesForEverySmellyFile();

        File smellyFile = new File(PATH_TO_SMELLY_FINAL_RESULT_FILE);

        if(isFileEmpty(smellyFile))
        {
           boolean removedSmelly = smellyFile.delete();
            File cleanFile = new File(PATH_TO_CLEAN_FINAL_RESULT_FILE);
            boolean removedClean = cleanFile.delete();
            if (removedSmelly && removedClean)
            {
                System.out.println("No Smelly Files found for this repository");
                System.out.println();
            }
        }

    }
}
