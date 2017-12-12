package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pipeline.Util.*;
import static pipeline.WholePipeline.*;

final class Git
{

    static void getAllCommitIdsAndCreateFileWithSortedIds() throws IOException, InterruptedException, ParseException
    {

        List<String> linesFromConsole = Git.retrieveCommits();
        removeFileIfPresent(REPOSITORY_HISTORY_FILE_PATH);
        for (String line : linesFromConsole)
        {
            writeLineToFile(line, REPOSITORY_HISTORY_FILE_PATH);
        }

        COMMIT_IDS_WITH_DATES = FileHandler.getAllCommitIdsFromConsoleLines(linesFromConsole);


        ArrayList<String> commitIds = new ArrayList<>(COMMIT_IDS_WITH_DATES.keySet());

        removeFileIfPresent(COMMIT_IDS_FILE_PATH);
        for (String commit : commitIds)
        {
            writeLineToFile(commit, COMMIT_IDS_FILE_PATH);
            COMMIT_IDS.add(commit);
        }
    }

    private static ArrayList<String> retrieveCommits() throws IOException, InterruptedException
    {
        //        return executeCommandsAndReadLinesFromConsole(new File(REPO_FOLDER_NAME), "git", "log", "--first-parent", "--reverse", "--pretty=format:%H=%ad=");
        return executeCommandsAndReadLinesFromConsole(new File(REPO_FOLDER_NAME), "git", "log", "--reverse", "--pretty=format:%H=%ad=");
    }


    static ArrayList<String> executeCommandsAndReadLinesFromConsole(File dir, String... command) throws IOException, InterruptedException
    {
        //log();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.directory(dir);
        Process process = processBuilder.start();

        ArrayList<String> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.add(line);
            }
        }


        return output;


    }


    @SuppressWarnings("unused")
    static long getNumDaysBtw2Dates(Calendar startDate, Calendar endDate)
    {
        Date end = endDate.getTime();
        Date start = startDate.getTime();
        long timeDiff = Math.abs(end.getTime() - start.getTime());
        return TimeUnit.MILLISECONDS.toDays(timeDiff);
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
            dateStringFromLine = dateStringFromLine.replaceAll(".-....$", "");
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


    private static String normalizeMonth(String month)
    {
        //Util.log();

        if (month.equalsIgnoreCase("Jan"))
            month = "01";
        if (month.equalsIgnoreCase("Feb"))
            month = "02";
        if (month.equalsIgnoreCase("Mar"))
            month = "03";
        if (month.equalsIgnoreCase("Apr"))
            month = "04";
        if (month.equalsIgnoreCase("May"))
            month = "05";
        if (month.equalsIgnoreCase("Jun"))
            month = "06";
        if (month.equalsIgnoreCase("Jul"))
            month = "07";
        if (month.equalsIgnoreCase("Aug"))
            month = "08";
        if (month.equalsIgnoreCase("Sep"))
            month = "09";
        if (month.equalsIgnoreCase("Oct"))
            month = "10";
        if (month.equalsIgnoreCase("Nov"))
            month = "11";
        if (month.equalsIgnoreCase("Dec"))
            month = "12";

        return month;
    }


}



