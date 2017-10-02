package pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static pipeline.WholePipeline.REPO_NAME;


class ResultFileWriter
{


    private final static String TAG = Git.class.getCanonicalName();
    //final static String ALL_HISTORY_FILE_NAME = PATH_TO_REPOSITORY + "/all_commits.txt";
    //final static String RESULT_FILE_NAME = PATH_TO_REPOSITORY + "/result.txt";

    static void writeLineToFile(String lineToAdd, String pathToFile) throws IOException
    {
        boolean is_file_created = false;
        File file = new File(pathToFile);
        if (!file.exists())
            is_file_created = file.createNewFile();

        if (file.exists() || is_file_created)

        {
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(lineToAdd);
            bw.newLine();
            bw.flush();
        }
    }

    static void log()
    {   //[2] -gets the previous method, hence, the one from which log() i called
        System.err.println("LOG: " + TAG + "." + Thread.currentThread().getStackTrace()[2].getMethodName());
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
