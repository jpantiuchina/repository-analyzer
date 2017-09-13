package pipeline;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Git
{

    final static File PATH_TO_REPOSITORY = new File("output/cloned_repository");


    static void clone_repository(String repository) throws IOException, InterruptedException
    {
        Process process = Runtime.getRuntime().exec("git clone " + repository + " " + PATH_TO_REPOSITORY);

        int returned = process.waitFor();

        if (returned == 0 && PATH_TO_REPOSITORY.isDirectory())
            System.err.println("Repository " + repository + " copied successfully");
        else if (PATH_TO_REPOSITORY.isDirectory())
            System.err.println("Repository " + repository + " was not copied, because it already exist.");
        else
        {
            ResultFileWriter.log();
            System.err.println("ERROR. Repository " + repository + " was not copied. " + "returned value: " + returned);
            System.exit(2);
        }
    }


     static void retrieveCommits() throws IOException
    {
        String operatingSystem = System.getProperty("os.name");

        if (operatingSystem.contains("Mac"))
        {
            //for Mac
            executeCommandsAndReadLinesFromConsole("/bin/bash", "-c", "cd " + PATH_TO_REPOSITORY + " && git log --reverse --pretty=format:'%h =%ad='");
        }
        else if (operatingSystem.contains("Windows"))
        {
            //for Windows
            executeCommandsAndReadLinesFromConsole("cmd /c cd " + PATH_TO_REPOSITORY + " && git log --reverse --pretty=format:\"%h =%ad=\" ");
        }
        else
        {
            ResultFileWriter.log();
            System.err.println("The program works only on Mac and Windows OS's");
            System.exit(1);
        }
    }




     static void executeCommandsAndReadLinesFromConsole(String... command) throws IOException
    {

        Process process = Runtime.getRuntime().exec(command);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        readCommandOutputAndWriteResultToFileAndArraList(stdInput, ResultFileWriter.ALL_HISTORY_FILE_NAME);
        readCommandOutputAndWriteResultToFileAndArraList(stdError, ResultFileWriter.ALL_HISTORY_FILE_NAME);

    }


    private static void readCommandOutputAndWriteResultToFileAndArraList(BufferedReader stdInput, String outputFilePath) throws IOException
    {
        // read the output from the command
        String command_output;
        while ((command_output = stdInput.readLine()) != null)
        {
            ResultFileWriter.writeLineToFile(command_output, outputFilePath);
            IntervalSplitter.linesFromConsole.add(command_output);
        }

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



