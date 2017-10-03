package pipeline;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import static pipeline.ResultFileWriter.OUTPUT_FOLDER_NAME;
import static pipeline.ResultFileWriter.removeFileIfPresent;
import static pipeline.Util.log;
import static pipeline.WholePipeline.*;
//import static pipeline.WholePipeline.OUTPUT_FOLDER_NAME;


final class Git
{


    static void cloneRepository(String repository) throws IOException, InterruptedException
    {
        log();
        FileUtils.deleteDirectory(PATH_TO_REPOSITORY);
        new File(PATH_TO_REPOSITORY.toString()).mkdir();
        executeCommandsAndReadLinesFromConsole(new File("."), "git", "clone", "--single-branch", repository, PATH_TO_REPOSITORY.toString());

    }



    static void retrieveWholeRepoHistory(String repositoryURL) throws IOException, InterruptedException, ParseException {

        //Git.cloneRepository(repositoryURL);

        List<String> linesFromConsole = Git.retrieveCommits();

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

    static ArrayList<String> retrieveCommits() throws IOException, InterruptedException
    {
        removeFileIfPresent(REPOSITORY_HISTORY_FILE_PATH);
        //log();
        return executeCommandsAndReadLinesFromConsole(PATH_TO_REPOSITORY, "git", "log", "--reverse", "--pretty=format:%H=%ad=");
    }



    static ArrayList<String> executeCommandsAndReadLinesFromConsole(File dir, String... command) throws IOException, InterruptedException
    {
        //log();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.directory(dir);
        Process process = processBuilder.start();

        ArrayList<String> output = new ArrayList<>();


        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));

        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.add(line);
                //Util.writeLineToFile(line, Util.ALL_HISTORY_FILE_NAME);
            }
        }
        finally
        {
            reader.close();

        }


        if (process.waitFor() != 0)
            throw new IOException("Command " + Arrays.toString(command) + " failed with exit code " + process.exitValue());


        return output;


    }


    static List<String> executeCommandsAndReadLinesFromConsoleOLD(String outputPathToFile, String... command) throws IOException
    {
        Process process = Runtime.getRuntime().exec(command);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        List<String> linesFromConsole = readCommandOutputAndWriteResultToFileAndReturnLinesFromConsole(stdInput, outputPathToFile);
        linesFromConsole.addAll(readCommandOutputAndWriteResultToFileAndReturnLinesFromConsole(stdError, outputPathToFile));

        return linesFromConsole;
    }



    private static List<String> readCommandOutputAndWriteResultToFileAndReturnLinesFromConsole(BufferedReader stdInput, String outputFilePath) throws IOException
    {
        List<String> linesFromConsole = new ArrayList<>();
        // read the output from the command
        String command_output;
        while ((command_output = stdInput.readLine()) != null)
        {
            ResultFileWriter.writeLineToFile(command_output, outputFilePath);
            linesFromConsole.add(command_output);
        }
        return linesFromConsole;
    }



    static long getNumDaysBtw2Dates(Calendar startDate, Calendar endDate)
    {
        Date end = endDate.getTime();
        Date start = startDate.getTime();
        long diff = 0;
        long timeDiff = Math.abs(start.getTime() - end.getTime());
        diff = TimeUnit.MILLISECONDS.toDays(timeDiff);
        return diff;
    }



    static Calendar getCommitDateAfterNDays(Calendar date, int days) throws ParseException
    {
        date.add(Calendar.DATE, days);
        return date;
    }

    private static String normalizeMonth(String month)
    {
        if (month.equalsIgnoreCase("Jan")) month = "01";
        if (month.equalsIgnoreCase("Feb")) month = "02";
        if (month.equalsIgnoreCase("Mar")) month = "03";
        if (month.equalsIgnoreCase("Apr")) month = "04";
        if (month.equalsIgnoreCase("May")) month = "05";
        if (month.equalsIgnoreCase("Jun")) month = "06";
        if (month.equalsIgnoreCase("Jul")) month = "07";
        if (month.equalsIgnoreCase("Aug")) month = "08";
        if (month.equalsIgnoreCase("Sep")) month = "09";
        if (month.equalsIgnoreCase("Oct")) month = "10";
        if (month.equalsIgnoreCase("Nov")) month = "11";
        if (month.equalsIgnoreCase("Dec")) month = "12";

        return month;
    }


    static Calendar readDateFromLine(String line) throws ParseException, IOException
    {
        //Util.log();

        Pattern pattern = Pattern.compile("=(.*?)=");
        Matcher matcher = pattern.matcher(line);

        Calendar calendarDate = null;

        if (matcher.find())
        {
            String dateStringFromLine = matcher.group(1);

            dateStringFromLine = dateStringFromLine.replaceFirst("....", "");
            dateStringFromLine = dateStringFromLine.replaceAll(".\\+....$", "");
            dateStringFromLine = dateStringFromLine.replaceFirst("...", normalizeMonth(dateStringFromLine.substring(0, 3)));

            String year = dateStringFromLine.substring(dateStringFromLine.lastIndexOf(' ') + 1);

            //remove year
            dateStringFromLine = dateStringFromLine.replaceAll(dateStringFromLine.substring(dateStringFromLine.lastIndexOf(' ') + 1), "");

            //add year at the beginning
            dateStringFromLine = year.concat(" " + dateStringFromLine);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
            Date date = sdf.parse(dateStringFromLine);

            calendarDate = Calendar.getInstance();
            calendarDate.setTime(date);
        }

        if (calendarDate == null)
        {
            log();
            System.err.print("Error. The calendar date was not created");
            System.exit(4);
        }
        return calendarDate;
    }


}



