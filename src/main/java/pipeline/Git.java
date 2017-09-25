package pipeline;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pipeline.ResultFileWriter.OUTPUT_FOLDER_NAME;
import static pipeline.ResultFileWriter.removeFileIfPresent;

final class Git
{

   static File createPathToRepository(String clonedRepoFolderName)
   {

       return new File( OUTPUT_FOLDER_NAME + clonedRepoFolderName);
   }


    static String clone_repository(String repositoryURL, String clonedRepoFolderName) throws IOException, InterruptedException
    {
        File PATH_TO_REPOSITORY = createPathToRepository(clonedRepoFolderName);

        Process process = Runtime.getRuntime().exec("git clone " + repositoryURL + " " + PATH_TO_REPOSITORY);

        System.out.println("Trying to clone repository: " + repositoryURL + " into folder " + PATH_TO_REPOSITORY);
        int returned = process.waitFor();

        if (returned == 0 && PATH_TO_REPOSITORY.isDirectory())
            System.out.println("Repository " + repositoryURL + " copied successfully");
        else if (PATH_TO_REPOSITORY.isDirectory())
            System.out.println("Repository " + repositoryURL + " was not copied, because it already exist.");
        else
        {
            ResultFileWriter.log();
            System.err.println("ERROR. Repository " + repositoryURL + " was not copied for unknown reason. " + "returned value: " + returned);
            System.exit(2);
        }
        return PATH_TO_REPOSITORY.toString();
    }




     static List<String> retrieveCommitsForRepoAndSaveResultsToFile(String inputPathToRepository, String outputPathToRepoHistoryFile) throws IOException
    {
        removeFileIfPresent(outputPathToRepoHistoryFile);

        String operatingSystem = System.getProperty("os.name");

        List<String> linesFromConsole = null;
        if (operatingSystem.contains("Mac"))
        {
            //for Mac
            linesFromConsole = executeCommandsAndReadLinesFromConsole(outputPathToRepoHistoryFile, "/bin/bash", "-c", "cd " + inputPathToRepository + " && git log --reverse --pretty=format:'%H =%ad='");
        }
        else if (operatingSystem.contains("Windows"))
        {
            //for Windows
            linesFromConsole = executeCommandsAndReadLinesFromConsole(outputPathToRepoHistoryFile, "cmd /c cd " + inputPathToRepository + " && git log --reverse --pretty=format:\"%H =%ad=\" ");
        }
        else
        {
            ResultFileWriter.log();
            System.err.println("This program works only on Mac and Windows OS's");
            System.exit(1);
        }
        return linesFromConsole;
    }




     static List<String> executeCommandsAndReadLinesFromConsole(String outputPathToFile, String... command) throws IOException
    {

        //System.out.println("Received command: " + Arrays.toString(command) + " Lines from console will be saved to " + outputPathToFile);
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


    static Calendar readDateFromLine(String line) throws ParseException, IOException
    {
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
            ResultFileWriter.log();
            System.err.print("Error. The calendar date was not created");
            System.exit(4);
        }
        return calendarDate;
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


}



