package pipeline;

import java.io.*;
import java.text.ParseException;
import java.util.Calendar;

import static pipeline.WholePipeline.*;


class Util
{

    private final static String TAG = Util.class.getCanonicalName();

    static void writeLineToFile(String lineToAdd, String pathToFile) throws IOException
    {
        try (PrintStream out = new PrintStream(new FileOutputStream(pathToFile, true)))
        {
            out.println(lineToAdd);
        }
    }


    static void log()
    {   //[2] - gets the previous method, which is the one from which log() i called
        System.err.println("LOG: " + TAG + "." + Thread.currentThread().getStackTrace()[2].getMethodName());

    }


    static void createPaths(String folderName) throws IOException
    {
        REPO_FOLDER_NAME = folderName.trim();//.concat("/");
        REPOSITORY_HISTORY_FILE_PATH = REPO_FOLDER_NAME + "linesFromConsole.txt";
        COMMIT_IDS_FILE_PATH = REPO_FOLDER_NAME + "sorted_commit_Ids.txt";

        System.out.println("REPOSITORY: " + REPO_FOLDER_NAME);
        System.out.println("SORTED COMMIT IDS WITH DATES: " + REPOSITORY_HISTORY_FILE_PATH);
        System.out.println("SORTED COMMIT IDS: " + COMMIT_IDS_FILE_PATH);

        File result = new File("result");
        //noinspection ResultOfMethodCallIgnored
        result.mkdir();

        String repoName = REPO_FOLDER_NAME.replaceFirst("^repos/", "");
        repoName = repoName.replaceAll("/$", "");

        String predictIn;
        if (IS_PREDICTION_IN_DAYS)
        {
            predictIn = "-days";
        } else
        {
            predictIn = "-commits";
        }

        PATH_TO_SMELLY_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(repoName).concat("-smelly").concat(predictIn).concat(".csv"));
        PATH_TO_CLEAN_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(repoName).concat("-clean").concat(predictIn).concat(".csv"));

        removeFileIfPresent(PATH_TO_SMELLY_FINAL_RESULT_FILE);
        removeFileIfPresent(PATH_TO_CLEAN_FINAL_RESULT_FILE);

        createEmptyFinalResultFiles();

    }


    static Calendar getDateBeforeOrAfterNDays(Calendar date, int days) throws ParseException
    {
        Calendar calendar = (Calendar) date.clone();
        calendar.add(Calendar.DATE, days);
        return calendar;
    }


    static boolean isFileEmpty(File inputFile) throws IOException
    {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
        {
            while (br.readLine() != null)
            {
                count++;
            }
        }
        return count < 2;
    }

/*
   static int MAX_INT_DAYS_OR_COMMITS = 120;
    static boolean IS_PREDICTION_IN_DAYS = true;
    static int STEP = 15;
 */

    private static void createEmptyFinalResultFiles() throws IOException
    {
        StringBuilder headerLine = new StringBuilder();
        int currentInterval = STEP;

        headerLine.append("filename,futureCommitId,");

        while (currentInterval <= MAX_INT_DAYS_OR_COMMITS)
        {
            headerLine.append("metricsCommitId").append(currentInterval).append(",");

            headerLine.append("LOC").append(currentInterval).append(",");
            headerLine.append("LCOM").append(currentInterval).append(",");
            headerLine.append("WMC").append(currentInterval).append(",");
            headerLine.append("RFC").append(currentInterval).append(",");
            headerLine.append("CBO").append(currentInterval).append(",");
            headerLine.append("NOM").append(currentInterval).append(",");
            headerLine.append("NOA").append(currentInterval).append(",");
            headerLine.append("DIT").append(currentInterval).append(",");
            headerLine.append("NOC").append(currentInterval).append(",");

            headerLine.append("LOCrecent").append(currentInterval).append(",");
            headerLine.append("LCOMrecent").append(currentInterval).append(",");
            headerLine.append("WMCrecent").append(currentInterval).append(",");
            headerLine.append("RFCrecent").append(currentInterval).append(",");
            headerLine.append("CBOrecent").append(currentInterval).append(",");
            headerLine.append("NOMrecent").append(currentInterval).append(",");
            headerLine.append("NOArecent").append(currentInterval).append(",");
            headerLine.append("DITrecent").append(currentInterval).append(",");
            headerLine.append("NOCrecent").append(currentInterval).append(",");

            headerLine.append("LOChist").append(currentInterval).append(",");
            headerLine.append("LCOMhist").append(currentInterval).append(",");
            headerLine.append("WMChist").append(currentInterval).append(",");
            headerLine.append("RFChist").append(currentInterval).append(",");
            headerLine.append("CBOhist").append(currentInterval).append(",");
            headerLine.append("NOMhist").append(currentInterval).append(",");
            headerLine.append("NOAhist").append(currentInterval).append(",");
            headerLine.append("DIThist").append(currentInterval).append(",");
            headerLine.append("NOChist").append(currentInterval).append(",");

            currentInterval = currentInterval + STEP;

        }

        String smells = "isBlob, isCDSBP, isComplexClass, isFuncDec, isSpaghCode";


        try (PrintStream ps = new PrintStream(PATH_TO_CLEAN_FINAL_RESULT_FILE))
        {
            ps.println(headerLine);
        }

        try (PrintStream ps = new PrintStream(PATH_TO_SMELLY_FINAL_RESULT_FILE))
        {
            ps.println(headerLine.append(smells));
        }

    }


    static void removeFileIfPresent(String fileName) throws IOException
    {
        File file = new File(fileName);
        if (file.exists())
        {
            if (!file.delete())
            {
                System.err.println("Error removing old file " + fileName);
                System.exit(5);
            }
        }
    }
}
