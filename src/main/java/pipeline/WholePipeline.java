package pipeline;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import static pipeline.FileHandler.addCommitFilesForEverySmellyFile;
import static pipeline.FileHandler.getAllFilesInFolder;
import static pipeline.FileHandler.handleAllFiles;
import static pipeline.Git.getAllCommitIdsAndCreateFileWithSortedIds;
import static pipeline.Util.createPaths;



public class WholePipeline {

    static ArrayList<String> SMELLY_FILE_NAMES = new ArrayList<>();

    static String REPO_FOLDER_NAME;
    static String REPOSITORY_HISTORY_FILE_PATH;
    static String COMMIT_IDS_FILE_PATH;

    static ArrayList<String> COMMIT_IDS = new ArrayList<>();
    static LinkedHashMap<String, Calendar> COMMIT_IDS_WITH_DATES = new LinkedHashMap<>();

    static String PATH_TO_SMELLY_FINAL_RESULT_FILE;
    static String PATH_TO_CLEAN_FINAL_RESULT_FILE;

    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        System.err.println("");
        System.err.println("----------------------------------------------------------------");
        Util.log();


        if (args.length != 1) {
            Util.log();
            System.out.println("Usage argument: <repository folder name>");
            System.exit(1);
        }

        String pathToRepositoryFolderWithAllData = args[0];

        createPaths(pathToRepositoryFolderWithAllData);

        System.out.println(REPO_FOLDER_NAME);
        System.out.println(REPOSITORY_HISTORY_FILE_PATH);
        System.out.println(COMMIT_IDS_FILE_PATH);

        getAllCommitIdsAndCreateFileWithSortedIds();

        File[] allFilesInFolder = getAllFilesInFolder(REPO_FOLDER_NAME.concat("/bad_smell"));

        handleAllFiles(allFilesInFolder);
        addCommitFilesForEverySmellyFile();

    }
}
