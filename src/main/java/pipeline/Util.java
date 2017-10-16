package pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pipeline.WholePipeline.*;
import static pipeline.WholePipeline.FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD;
import static pipeline.WholePipeline.PATH_TO_REPOSITORY;



class Util
{

    private final static String TAG = Git.class.getCanonicalName();

    /*
    Equivalent to: PrintStream out = new PrintStream(new FileOutputStream(pathToFile, true))
try { out.println(lineToAdd); } finally { out.close(); }
try-with-resources
     */
    static void writeLineToFile(String lineToAdd, String pathToFile) throws IOException
    {
        try (PrintStream out = new PrintStream(new FileOutputStream(pathToFile, true)))
        {
            out.println(lineToAdd);
        }
    }



    static void log()
    {   //[2] - gets the previous method, hence, the one from which log() i called
        System.err.println("LOG: " + TAG + "." + Thread.currentThread().getStackTrace()[2].getMethodName());
    }


    static void createPaths()
    {
        OUTPUT_FOLDER_NAME = "history/output-" + REPO_NAME + "/";

        PATH_TO_REPOSITORY = new File(OUTPUT_FOLDER_NAME + REPO_NAME);

        REPOSITORY_HISTORY_FILE_PATH = PATH_TO_REPOSITORY + "/linesFromConsole.txt";
        COMMIT_IDS_FILE_PATH = PATH_TO_REPOSITORY + "/sorted_commit_Ids.txt";
        FILE_PATH_TO_LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT = PATH_TO_REPOSITORY + "/linesFromConsoleOnCommitsCheckout.txt";
        FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD = PATH_TO_REPOSITORY + "/LinesFromConsoleRunningPmd.txt";
    }


    static String getResultFileNameFromRepositoryURL(String url)
    {
        Util.log();

        final Pattern pattern = Pattern.compile("([^\\/]+)$");
        final Matcher matcher = pattern.matcher(url);

        String filename = null;
        if (matcher.find())
        {
            filename = matcher.group(0);
        }
        else
        {
            System.err.println("Impossible to generate result filename from repository");
            System.exit(4);
        }


        return (filename);
    }


    static Calendar getCommitDateBeforeOrAfterNDays(Calendar date, int days) throws ParseException {
                 //Util.log();
                 date.add(Calendar.DATE, days);
                 return date;
             }

    static void createEmptyFinalResultFiles() throws IOException
    {
        //removes previous result file
        File result = new File("result");

        if (result.mkdir())
        {
            System.out.print(result.toString() + " folder created. ");
        }
        else
        {
            System.out.print(result.toString() + " folder was not created because it already exist. ");
        }

        PATH_TO_SMELLY_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(REPO_NAME).concat("-smelly").concat(".csv"));
        PATH_TO_CLEAN_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(REPO_NAME).concat("-clean").concat(".csv"));


        System.out.println("Results will be saved (or overwritten) to file " + PATH_TO_SMELLY_FINAL_RESULT_FILE);

        try (PrintStream ps = new PrintStream(PATH_TO_SMELLY_FINAL_RESULT_FILE)) {
            ps.println("fileName, futureCommitId," +
                            "CBO15,      WMC15,      DIT15,      NOC15,      RFC15,      LCOM15,      NOM15,      NOPM15,      NOSM15,      NOF15,      NOPF15,      NOSF15,      NOSI15,      LOC15," +
                            "CBORecentWhenInterval15,      WMCRecentWhenInterval15,      DITRecentWhenInterval15,      NOCRecentWhenInterval15,      RFCRecentWhenInterval15,      LCOMRecentWhenInterval15,      NOMRecentWhenInterval15,      NOPMRecentWhenInterval15,      NOSMRecentWhenInterval15,      NOFRecentWhenInterval15,      NOPFRecentWhenInterval15,      NOSFRecentWhenInterval15,      NOSIRecentWhenInterval15,      LOCRecentWhenInterval15," +
                            "CBOslopeAllHistory15, WMCslopeAllHistory15, DITslopeAllHistory15, NOCslopeAllHistory15, RFCslopeAllHistory15, LCOMslopeAllHistory15, NOMslopeAllHistory15, NOPMslopeAllHistory15, NOSMslopeAllHistory15, NOFslopeAllHistory15, NOPFslopeAllHistory15, NOSFslopeAllHistory15, NOSIslopeAllHistory15, LOCslopeAllHistory15, " +

                            "CBO30,      WMC30,      DIT30,      NOC30,      RFC30,      LCOM30,      NOM30,      NOPM30,      NOSM30,      NOF30,      NOPF30,      NOSF30,      NOSI30,      LOC30," +
                            "CBORecentWhenInterval30,      WMCRecentWhenInterval30,      DITRecentWhenInterval30,      NOCRecentWhenInterval30,      RFCRecentWhenInterval30,      LCOMRecentWhenInterval30,      NOMRecentWhenInterval30,      NOPMRecentWhenInterval30,      NOSMRecentWhenInterval30,      NOFRecentWhenInterval30,      NOPFRecentWhenInterval30,      NOSFRecentWhenInterval30,      NOSIRecentWhenInterval30,      LOCRecentWhenInterval30," +
                            "CBOslopeAllHistory30, WMCslopeAllHistory30, DITslopeAllHistory30, NOCslopeAllHistory30, RFCslopeAllHistory30, LCOMslopeAllHistory30, NOMslopeAllHistory30, NOPMslopeAllHistory30, NOSMslopeAllHistory30, NOFslopeAllHistory30, NOPFslopeAllHistory30, NOSFslopeAllHistory30, NOSIslopeAllHistory30, LOCslopeAllHistory30, " +

                            "CBO60,      WMC60,      DIT60,      NOC60,      RFC60,      LCOM60,      NOM60,      NOPM60,      NOSM60,      NOF60,      NOPF60,      NOSF60,      NOSI60,      LOC60," +
                            "CBORecentWhenInterval60,      WMCRecentWhenInterval60,      DITRecentWhenInterval60,      NOCRecentWhenInterval60,      RFCRecentWhenInterval60,      LCOMRecentWhenInterval60,      NOMRecentWhenInterval60,      NOPMRecentWhenInterval60,      NOSMRecentWhenInterval60,      NOFRecentWhenInterval60,      NOPFRecentWhenInterval60,      NOSFRecentWhenInterval60,      NOSIRecentWhenInterval60,      LOCRecentWhenInterval60," +
                            "CBOslopeAllHistory60, WMCslopeAllHistory60, DITslopeAllHistory60, NOCslopeAllHistory60, RFCslopeAllHistory60, LCOMslopeAllHistory60, NOMslopeAllHistory60, NOPMslopeAllHistory60, NOSMslopeAllHistory60, NOFslopeAllHistory60, NOPFslopeAllHistory60, NOSFslopeAllHistory60, NOSIslopeAllHistory60, LOCslopeAllHistory60, " +

                            "CBO90,      WMC90,      DIT90,      NOC90,      RFC90,      LCOM90,      NOM90,      NOPM90,      NOSM90,      NOF90,      NOPF90,      NOSF90,      NOSI90,      LOC90," +
                            "CBORecentWhenInterval90,      WMCRecentWhenInterval90,      DITRecentWhenInterval90,      NOCRecentWhenInterval90,      RFCRecentWhenInterval90,      LCOMRecentWhenInterval90,      NOMRecentWhenInterval90,      NOPMRecentWhenInterval90,      NOSMRecentWhenInterval90,      NOFRecentWhenInterval90,      NOPFRecentWhenInterval90,      NOSFRecentWhenInterval90,      NOSIRecentWhenInterval90,      LOCRecentWhenInterval90," +
                            "CBOslopeAllHistory90, WMCslopeAllHistory90, DITslopeAllHistory90, NOCslopeAllHistory90, RFCslopeAllHistory90, LCOMslopeAllHistory90, NOMslopeAllHistory90, NOPMslopeAllHistory90, NOSMslopeAllHistory90, NOFslopeAllHistory90, NOPFslopeAllHistory90, NOSFslopeAllHistory90, NOSIslopeAllHistory90, LOCslopeAllHistory90, " +

                            "CBO120,      WMC120,      DIT120,      NOC120,      RFC120,      LCOM120,      NOM120,      NOPM120,      NOSM120,      NOF120,      NOPF120,      NOSF120,      NOSI120,      LOC120," +
                            "CBORecentWhenInterval120,      WMCRecentWhenInterval120,      DITRecentWhenInterval120,      NOCRecentWhenInterval120,      RFCRecentWhenInterval120,      LCOMRecentWhenInterval120,      NOMRecentWhenInterval120,      NOPMRecentWhenInterval120,      NOSMRecentWhenInterval120,      NOFRecentWhenInterval120,      NOPFRecentWhenInterval120,      NOSFRecentWhenInterval120,      NOSIRecentWhenInterval120,      LOCRecentWhenInterval120," +
                            "CBOslopeAllHistory120, WMCslopeAllHistory120, DITslopeAllHistory120, NOCslopeAllHistory120, RFCslopeAllHistory120, LCOMslopeAllHistory120, NOMslopeAllHistory120, NOPMslopeAllHistory120, NOSMslopeAllHistory120, NOFslopeAllHistory120, NOPFslopeAllHistory120, NOSFslopeAllHistory120, NOSIslopeAllHistory120, LOCslopeAllHistory120, " +
                    "isBlob, isCoupling, isNPath"

            );
        }

        System.out.println("Results will be saved (or overwritten) to file " + PATH_TO_CLEAN_FINAL_RESULT_FILE);

        try (PrintStream ps = new PrintStream(PATH_TO_CLEAN_FINAL_RESULT_FILE)) {
            ps.println("fileName, futureCommitId," +
                    "CBO15,      WMC15,      DIT15,      NOC15,      RFC15,      LCOM15,      NOM15,      NOPM15,      NOSM15,      NOF15,      NOPF15,      NOSF15,      NOSI15,      LOC15," +
                    "CBORecentWhenInterval15,      WMCRecentWhenInterval15,      DITRecentWhenInterval15,      NOCRecentWhenInterval15,      RFCRecentWhenInterval15,      LCOMRecentWhenInterval15,      NOMRecentWhenInterval15,      NOPMRecentWhenInterval15,      NOSMRecentWhenInterval15,      NOFRecentWhenInterval15,      NOPFRecentWhenInterval15,      NOSFRecentWhenInterval15,      NOSIRecentWhenInterval15,      LOCRecentWhenInterval15," +
                    "CBOslopeAllHistory15, WMCslopeAllHistory15, DITslopeAllHistory15, NOCslopeAllHistory15, RFCslopeAllHistory15, LCOMslopeAllHistory15, NOMslopeAllHistory15, NOPMslopeAllHistory15, NOSMslopeAllHistory15, NOFslopeAllHistory15, NOPFslopeAllHistory15, NOSFslopeAllHistory15, NOSIslopeAllHistory15, LOCslopeAllHistory15, " +

                    "CBO30,      WMC30,      DIT30,      NOC30,      RFC30,      LCOM30,      NOM30,      NOPM30,      NOSM30,      NOF30,      NOPF30,      NOSF30,      NOSI30,      LOC30," +
                    "CBORecentWhenInterval30,      WMCRecentWhenInterval30,      DITRecentWhenInterval30,      NOCRecentWhenInterval30,      RFCRecentWhenInterval30,      LCOMRecentWhenInterval30,      NOMRecentWhenInterval30,      NOPMRecentWhenInterval30,      NOSMRecentWhenInterval30,      NOFRecentWhenInterval30,      NOPFRecentWhenInterval30,      NOSFRecentWhenInterval30,      NOSIRecentWhenInterval30,      LOCRecentWhenInterval30," +
                    "CBOslopeAllHistory30, WMCslopeAllHistory30, DITslopeAllHistory30, NOCslopeAllHistory30, RFCslopeAllHistory30, LCOMslopeAllHistory30, NOMslopeAllHistory30, NOPMslopeAllHistory30, NOSMslopeAllHistory30, NOFslopeAllHistory30, NOPFslopeAllHistory30, NOSFslopeAllHistory30, NOSIslopeAllHistory30, LOCslopeAllHistory30, " +

                    "CBO60,      WMC60,      DIT60,      NOC60,      RFC60,      LCOM60,      NOM60,      NOPM60,      NOSM60,      NOF60,      NOPF60,      NOSF60,      NOSI60,      LOC60," +
                    "CBORecentWhenInterval60,      WMCRecentWhenInterval60,      DITRecentWhenInterval60,      NOCRecentWhenInterval60,      RFCRecentWhenInterval60,      LCOMRecentWhenInterval60,      NOMRecentWhenInterval60,      NOPMRecentWhenInterval60,      NOSMRecentWhenInterval60,      NOFRecentWhenInterval60,      NOPFRecentWhenInterval60,      NOSFRecentWhenInterval60,      NOSIRecentWhenInterval60,      LOCRecentWhenInterval60," +
                    "CBOslopeAllHistory60, WMCslopeAllHistory60, DITslopeAllHistory60, NOCslopeAllHistory60, RFCslopeAllHistory60, LCOMslopeAllHistory60, NOMslopeAllHistory60, NOPMslopeAllHistory60, NOSMslopeAllHistory60, NOFslopeAllHistory60, NOPFslopeAllHistory60, NOSFslopeAllHistory60, NOSIslopeAllHistory60, LOCslopeAllHistory60, " +

                    "CBO90,      WMC90,      DIT90,      NOC90,      RFC90,      LCOM90,      NOM90,      NOPM90,      NOSM90,      NOF90,      NOPF90,      NOSF90,      NOSI90,      LOC90," +
                    "CBORecentWhenInterval90,      WMCRecentWhenInterval90,      DITRecentWhenInterval90,      NOCRecentWhenInterval90,      RFCRecentWhenInterval90,      LCOMRecentWhenInterval90,      NOMRecentWhenInterval90,      NOPMRecentWhenInterval90,      NOSMRecentWhenInterval90,      NOFRecentWhenInterval90,      NOPFRecentWhenInterval90,      NOSFRecentWhenInterval90,      NOSIRecentWhenInterval90,      LOCRecentWhenInterval90," +
                    "CBOslopeAllHistory90, WMCslopeAllHistory90, DITslopeAllHistory90, NOCslopeAllHistory90, RFCslopeAllHistory90, LCOMslopeAllHistory90, NOMslopeAllHistory90, NOPMslopeAllHistory90, NOSMslopeAllHistory90, NOFslopeAllHistory90, NOPFslopeAllHistory90, NOSFslopeAllHistory90, NOSIslopeAllHistory90, LOCslopeAllHistory90, " +

                    "CBO120,      WMC120,      DIT120,      NOC120,      RFC120,      LCOM120,      NOM120,      NOPM120,      NOSM120,      NOF120,      NOPF120,      NOSF120,      NOSI120,      LOC120," +
                    "CBORecentWhenInterval120,      WMCRecentWhenInterval120,      DITRecentWhenInterval120,      NOCRecentWhenInterval120,      RFCRecentWhenInterval120,      LCOMRecentWhenInterval120,      NOMRecentWhenInterval120,      NOPMRecentWhenInterval120,      NOSMRecentWhenInterval120,      NOFRecentWhenInterval120,      NOPFRecentWhenInterval120,      NOSFRecentWhenInterval120,      NOSIRecentWhenInterval120,      LOCRecentWhenInterval120," +
                    "CBOslopeAllHistory120, WMCslopeAllHistory120, DITslopeAllHistory120, NOCslopeAllHistory120, RFCslopeAllHistory120, LCOMslopeAllHistory120, NOMslopeAllHistory120, NOPMslopeAllHistory120, NOSMslopeAllHistory120, NOFslopeAllHistory120, NOPFslopeAllHistory120, NOSFslopeAllHistory120, NOSIslopeAllHistory120, LOCslopeAllHistory120 "

            );
        }

    }


}
