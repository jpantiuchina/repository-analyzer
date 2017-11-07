package pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Calendar;


import static pipeline.WholePipeline.*;


class Util
{

    private final static String TAG = Git.class.getCanonicalName();

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


    static void createPaths(String folderName) throws IOException {
        REPO_FOLDER_NAME = folderName.trim();
        REPOSITORY_HISTORY_FILE_PATH = REPO_FOLDER_NAME + "linesFromConsole.txt";
        COMMIT_IDS_FILE_PATH = REPO_FOLDER_NAME + "sorted_commit_Ids.txt";

        File result = new File("result");

        if (result.mkdir())
        {
            System.out.print(result.toString() + " folder was created. ");
        }
        else
        {
            System.out.print("file will be saved to the " + result.toString());
        }

        String repoName = REPO_FOLDER_NAME.replaceFirst("^repos/","");
        repoName = repoName.replaceAll("/$","");

        PATH_TO_SMELLY_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(repoName).concat("-smelly").concat(".csv"));
        PATH_TO_CLEAN_FINAL_RESULT_FILE = ((result.toString()).concat("/").concat(repoName).concat("-clean").concat(".csv"));

        removeFileIfPresent(PATH_TO_SMELLY_FINAL_RESULT_FILE);
        removeFileIfPresent(PATH_TO_CLEAN_FINAL_RESULT_FILE);

        createEmptyFinalResultFiles();

    }


    static Calendar getDateBeforeOrAfterNDays(Calendar date, int days) throws ParseException {
                Calendar calendar = (Calendar) date.clone();
                 calendar.add(Calendar.DATE, days);
                 return calendar;
             }


    private static void createEmptyFinalResultFiles() throws IOException
    {

        System.out.println("Results will be saved (or overwritten) to file " + PATH_TO_SMELLY_FINAL_RESULT_FILE);

        String data = "filename,commitId," +
                "LOC15,LCOM15,WMC15,RFC15,CBO15,NOM15,NOA15,DIT15,NOC15,"+
                "LOC15recent,LCOM15recent,WMC15recent,RFC15recent,CBO15recent,NOM15recent,NOA15recent,DIT15recent,NOC15recent,"+
                "LOC15hist,LCOM15hist,WMC15hist,RFC15hist,CBO15hist,NOM15hist,NOA15hist,DIT15hist,NOC15hist,"+

                "LOC30,LCOM30,WMC30,RFC30,CBO30,NOM30,NOA30,DIT30,NOC30,"+
                "LOC30recent,LCOM30recent,WMC30recent,RFC30recent,CBO30recent,NOM30recent,NOA30recent,DIT30recent,NOC30recent,"+
                "LOC30hist,LCOM30hist,WMC30hist,RFC30hist,CBO30hist,NOM30hist,NOA30hist,DIT30hist,NOC30hist,"+

                "LOC45,LCOM45,WMC45,RFC45,CBO45,NOM45,NOA45,DIT45,NOC45,"+
                "LOC45recent,LCOM45recent,WMC45recent,RFC45recent,CBO45recent,NOM45recent,NOA45recent,DIT45recent,NOC45recent,"+
                "LOC45hist,LCOM45hist,WMC45hist,RFC45hist,CBO45hist,NOM45hist,NOA45hist,DIT45hist,NOC45hist,"+

                "LOC60,LCOM60,WMC60,RFC60,CBO60,NOM60,NOA60,DIT60,NOC60,"+
                "LOC60recent,LCOM60recent,WMC60recent,RFC60recent,CBO60recent,NOM60recent,NOA60recent,DIT60recent,NOC60recent,"+
                "LOC60hist,LCOM60hist,WMC60hist,RFC60hist,CBO60hist,NOM60hist,NOA60hist,DIT60hist,NOC60hist,"+

                "LOC75,LCOM75,WMC75,RFC75,CBO75,NOM75,NOA75,DIT75,NOC75,"+
                "LOC75recent,LCOM75recent,WMC75recent,RFC75recent,CBO75recent,NOM75recent,NOA75recent,DIT75recent,NOC75recent,"+
                "LOC75hist,LCOM75hist,WMC75hist,RFC75hist,CBO75hist,NOM75hist,NOA75hist,DIT75hist,NOC75hist,"+

                "LOC90,LCOM90,WMC90,RFC90,CBO90,NOM90,NOA90,DIT90,NOC90,"+
                "LOC90recent,LCOM90recent,WMC90recent,RFC90recent,CBO90recent,NOM90recent,NOA90recent,DIT90recent,NOC90recent,"+
                "LOC90hist,LCOM90hist,WMC90hist,RFC90hist,CBO90hist,NOM90hist,NOA90hist,DIT90hist,NOC90hist,"+

                "LOC105,LCOM105,WMC105,RFC105,CBO105,NOM105,NOA105,DIT105,NOC105,"+
                "LOC105recent,LCOM105recent,WMC105recent,RFC105recent,CBO105recent,NOM105recent,NOA105recent,DIT105recent,NOC105recent,"+
                "LOC105hist,LCOM105hist,WMC105hist,RFC105hist,CBO105hist,NOM105hist,NOA105hist,DIT105hist,NOC105hist,"+

                "LOC120,LCOM120,WMC120,RFC120,CBO120,NOM120,NOA120,DIT120,NOC120,"+
                "LOC120recent,LCOM120recent,WMC120recent,RFC120recent,CBO120recent,NOM120recent,NOA120recent,DIT120recent,NOC120recent,"+
                "LOC120hist,LCOM120hist,WMC120hist,RFC120hist,CBO120hist,NOM120hist,NOA120hist,DIT120hist,NOC120hist,";


        String smells = "isBlob, isCDSBP, isComplexClass, isFuncDec, isSpaghCode";

        try (PrintStream ps = new PrintStream(PATH_TO_SMELLY_FINAL_RESULT_FILE)) {
            ps.println(data.concat(smells));
        }

        System.out.println("Results will be saved (or overwritten) to file " + PATH_TO_CLEAN_FINAL_RESULT_FILE);

        try (PrintStream ps = new PrintStream(PATH_TO_CLEAN_FINAL_RESULT_FILE)) {
            ps.println(data);
        }

    }


    static void removeFileIfPresent(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists())
        {
            if(file.delete())
            {
                System.out.println("file " + fileName + " was rewritten");
            }
            else
            {
                System.err.println("Error removing old file " + fileName);
                System.exit(5);
            }
        }
    }
}
