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
        PATH_TO_OTHER_SMELLY_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(repoName).concat("-other").concat(predictIn).concat(".csv"));


        createEmptyFinalResultFiles();
    }


    static Calendar getDateBeforeOrAfterNDays(Calendar date, int days) throws ParseException
    {
        Calendar calendar = (Calendar) date.clone();
        calendar.add(Calendar.DATE, days);
        return calendar;
    }


    private static boolean isFileEmpty(File inputFile) throws IOException
    {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
        {
            String line;
            while ((line = br.readLine()) != null && !line.trim().isEmpty())
            {
                count++;
            }
        }
        return count < 2;
    }


    static void removeEmptyFiles() throws IOException
    {
        File smellyFile = new File(PATH_TO_SMELLY_FINAL_RESULT_FILE);
        File cleanFile = new File(PATH_TO_CLEAN_FINAL_RESULT_FILE);
        File otherFile = new File(PATH_TO_OTHER_SMELLY_FINAL_RESULT_FILE);



        if(isFileEmpty(smellyFile) || isFileEmpty(cleanFile) )
        {
            boolean removedSmelly = smellyFile.delete();
            boolean removedClean = cleanFile.delete();
            boolean removedOther = otherFile.delete();


            if (removedSmelly && removedClean && removedOther)
            {
                System.out.println("No 'Smelly' Or 'Clean' Files found with needed minimum survival for repository: " + REPO_FOLDER_NAME );
                System.out.println();
            }
        }
    }

    private static void createEmptyFinalResultSmellyFile() throws FileNotFoundException
    {
        StringBuilder headerLine = new StringBuilder();
        int currentInterval = STEP;

        headerLine.append("filename, CommitCountWhenBecomesSmelly, CommitIdWhenBecomesSmelly ");

        while (currentInterval <= MAX_INT_DAYS_OR_COMMITS)
        {
            headerLine.append(",metricsCommitId").append(currentInterval).append(",");
            headerLine.append("metricsCommitCount").append(currentInterval).append(",");
            headerLine.append("predictionCommitId").append(currentInterval).append(",");
            headerLine.append("predictionCommitCount").append(currentInterval).append(",");

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
            headerLine.append("NOChist").append(currentInterval);

            currentInterval = currentInterval + STEP;

        }

        String smells = ",isBlob, isCDSBP, isComplexClass, isFuncDec, isSpaghCode";
        try (PrintStream ps = new PrintStream(PATH_TO_SMELLY_FINAL_RESULT_FILE))
        {
            ps.println(headerLine.append(smells));
        }
    }

    private static void createEmptyFinalResultOtherSmellyFile() throws FileNotFoundException
    {
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder headerLine = new StringBuilder();

        headerLine.append("filename, metricsCommitId,  metricsCommitCount,  predictionCommitId,  predictionCommitCount," +
                "becomesCommitId,  becomesCommitCount,");

        headerLine.append("isBlob, isCDSBP, isComplexCLass, isFuncDec, isSpaghCode,");

        headerLine.append("LOC").append(",");
        headerLine.append("LCOM").append(",");
        headerLine.append("WMC").append(",");
        headerLine.append("RFC").append(",");
        headerLine.append("CBO").append(",");
        headerLine.append("NOM").append(",");
        headerLine.append("NOA").append(",");
        headerLine.append("DIT").append(",");
        headerLine.append("NOC").append(",");

        headerLine.append("LOCrecent").append(",");
        headerLine.append("LCOMrecent").append(",");
        headerLine.append("WMCrecent").append(",");
        headerLine.append("RFCrecent").append(",");
        headerLine.append("CBOrecent").append(",");
        headerLine.append("NOMrecent").append(",");
        headerLine.append("NOArecent").append(",");
        headerLine.append("DITrecent").append(",");
        headerLine.append("NOCrecent").append(",");

        headerLine.append("LOChist").append(",");
        headerLine.append("LCOMhist").append(",");
        headerLine.append("WMChist").append(",");
        headerLine.append("RFChist").append(",");
        headerLine.append("CBOhist").append(",");
        headerLine.append("NOMhist").append(",");
        headerLine.append("NOAhist").append(",");
        headerLine.append("DIThist").append(",");
        headerLine.append("NOChist");


        String cleanHeaderLine = headerLine.toString().replaceAll(",$", "");
        try (PrintStream ps = new PrintStream(PATH_TO_OTHER_SMELLY_FINAL_RESULT_FILE))
        {
            ps.println(cleanHeaderLine);
        }
    }


    private static void createEmptyFinalResultCleanFile() throws IOException
    {
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder headerLine = new StringBuilder();

        headerLine.append("filename, metricsCommitId,  metricsCommitCount,  predictionCommitId,  predictionCommitCount,");

            headerLine.append("LOC").append(",");
            headerLine.append("LCOM").append(",");
            headerLine.append("WMC").append(",");
            headerLine.append("RFC").append(",");
            headerLine.append("CBO").append(",");
            headerLine.append("NOM").append(",");
            headerLine.append("NOA").append(",");
            headerLine.append("DIT").append(",");
            headerLine.append("NOC").append(",");

            headerLine.append("LOCrecent").append(",");
            headerLine.append("LCOMrecent").append(",");
            headerLine.append("WMCrecent").append(",");
            headerLine.append("RFCrecent").append(",");
            headerLine.append("CBOrecent").append(",");
            headerLine.append("NOMrecent").append(",");
            headerLine.append("NOArecent").append(",");
            headerLine.append("DITrecent").append(",");
            headerLine.append("NOCrecent").append(",");

            headerLine.append("LOChist").append(",");
            headerLine.append("LCOMhist").append(",");
            headerLine.append("WMChist").append(",");
            headerLine.append("RFChist").append(",");
            headerLine.append("CBOhist").append(",");
            headerLine.append("NOMhist").append(",");
            headerLine.append("NOAhist").append(",");
            headerLine.append("DIThist").append(",");
            headerLine.append("NOChist");


        String cleanHeaderLine = headerLine.toString().replaceAll(",$", "");
        try (PrintStream ps = new PrintStream(PATH_TO_CLEAN_FINAL_RESULT_FILE))
        {
            ps.println(cleanHeaderLine);
        }

    }

    private static void createEmptyFinalResultFiles() throws IOException
    {
        removeFileIfPresent(PATH_TO_SMELLY_FINAL_RESULT_FILE);
        removeFileIfPresent(PATH_TO_CLEAN_FINAL_RESULT_FILE);
        removeFileIfPresent(PATH_TO_OTHER_SMELLY_FINAL_RESULT_FILE);


        createEmptyFinalResultSmellyFile();

        createEmptyFinalResultCleanFile();

        createEmptyFinalResultOtherSmellyFile();
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
