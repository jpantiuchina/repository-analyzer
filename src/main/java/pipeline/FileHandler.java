package pipeline;

import com.github.mauricioaniche.ck.Runner;

import java.io.*;
import java.text.ParseException;
import java.util.*;

//import static pipeline.Git.OUTPUT_FOLDER_NAME;
import static pipeline.Git.getNumDaysBtw2Dates;
import static pipeline.Git.readDateFromLine;
import static pipeline.ResultFileWriter.*;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;
import static pipeline.Util.createEmptyFinalResultFiles;
import static pipeline.Util.getCommitDateBeforeOrAfterNDays;
import static pipeline.Util.writeLineToFile;
import static pipeline.WholePipeline.*;
//import static pipeline.WholePipeline.OUTPUT_FOLDER_NAME;


class FileHandler
{

    static void handleFilesInCommits() throws IOException, ParseException {
        createEmptyFinalResultFiles();

        ArrayList<String> allFileNamesInCommit = new ArrayList<>();
        for (String commitID : COMMIT_IDS)
        {
            allFileNamesInCommit = getAllFileNamesInCommit(commitID);
            for (String fileNameInCommit : allFileNamesInCommit) //combination of file and commitId
            {
                handleFileInCommit(fileNameInCommit, commitID); //save file data to clean or smelly result file
            }
        }
    }


    private static void handleFileInCommit(String fileName, String commitId) throws IOException, ParseException {
        ArrayList <String> smells = getSmellsForFileInCommit(fileName,commitId);
        if (smells.contains("true")) //save to smelly file
        {
            if(!SMELLY_FILE_NAMES.contains(fileName))
            {
                SMELLY_FILE_NAMES.add(fileName);
                // add to smelly result file
                String finalLine = createFinalFileDataLine(fileName, commitId, true);

                // add finalLine to output file
                if (finalLine != "")
                    writeLineToFile(finalLine, PATH_TO_SMELLY_FINAL_RESULT_FILE);

            }

        }
        else //save only clean files. Ignore those files that now are clean, but were smelly
        {
            if(!SMELLY_FILE_NAMES.contains(fileName))
            {
                // add to clean result file
                String finalLine = createFinalFileDataLine(fileName, commitId,false);

              //  System.out.println("FINAL LINE: " + finalLine);
              //  System.out.println("PATH: " + PATH_TO_CLEAN_FINAL_RESULT_FILE);
                // add finalLine to output file
                if (finalLine != "")
                    writeLineToFile(finalLine, PATH_TO_CLEAN_FINAL_RESULT_FILE);
            }
        }

    }

    private static String getCommiIDBeforeDate(Calendar date)
    {
        int count = 0;
        String commitId = COMMIT_IDS.get(count);
        Calendar commitDate = COMMIT_IDS_WITH_DATES.get(commitId);
        String previousCommitId = "";
        while (commitDate.before(date) && count < COMMIT_IDS.size() - 1)
        {
            previousCommitId = commitId;
            count++;
            commitId = COMMIT_IDS.get(count);
            commitDate = COMMIT_IDS_WITH_DATES.get(commitId);
        }
        return previousCommitId;
    }



    private static String getCommitIdWhenFileWasAdded(String fileName) throws IOException
    {
        String addedInCommitId = "";
        boolean foundWhenAdded = false;
        if (removeCommitsInWhichFileWasNotPresent(fileName, COMMIT_IDS).size() > 0) {
            for (String commitId : COMMIT_IDS) {
                if (!foundWhenAdded)
                {
                    ArrayList<String> fileNamesInCommit = getAllFileNamesInCommit(commitId);
                    if (fileNamesInCommit.contains(fileName)) {
                        foundWhenAdded = true;
                        addedInCommitId = commitId;
                    }
                }
            }
        }
        if (!foundWhenAdded)
        {
            log();
            System.out.println("Error. Not found when file was added.");
            System.exit(3);
        }

        return addedInCommitId;
    }


    private static String getCommitIDBeforeNDays(String futureCommitId, int interval) throws ParseException {
        Calendar futureCommitDate = COMMIT_IDS_WITH_DATES.get(futureCommitId);
        Calendar metricsDate = getCommitDateBeforeOrAfterNDays(futureCommitDate, -interval);
        String metricsCommitId = getCommiIDBeforeDate(metricsDate);

        return metricsCommitId;
    }

    private static ArrayList<Double> getFileSlopesForAllMetrics(String fileName, String beginCommitIncluding, String lastCommitExcluding) throws IOException
    {
        ArrayList<Double> slopes = null;

        ArrayList<String> slopeCommits = getAllCommitsBetween(beginCommitIncluding, lastCommitExcluding);
        slopeCommits = removeCommitsInWhichFileWasNotPresent(fileName, slopeCommits);

        if (slopeCommits.size() > 1) // to calculate the slope at least 2 commits needed
        {

            ArrayList<ArrayList<Double>> metricsForFileHistory = getQualityMetricsForFileInCommits(fileName, slopeCommits);
            slopes = getSlopeForAllFileMetrics(metricsForFileHistory);
        }

        return slopes;
    }


    private static Map<String,ArrayList<Double>> getMetricsHistorySlopesRecentSlopesForFileInInterval(String fileName, int interval, String futureCommitId) throws IOException, ParseException {

        ArrayList<Double> slopesHistory = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)); //if there are no history or one past commit, the slope can't be calculated
        ArrayList<Double> slopesRecent = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0));
        ArrayList<Double> metrics = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0));

        Map<String,ArrayList<Double>> map = new HashMap();

        int futureCommitCount = COMMIT_IDS.indexOf(futureCommitId);
        Calendar futureCommitDate = COMMIT_IDS_WITH_DATES.get(futureCommitId);

        String firstCommitId = COMMIT_IDS_WITH_DATES.keySet().iterator().next();
        Calendar firstCommitDate = COMMIT_IDS_WITH_DATES.get(firstCommitId);
        long numOfDaysFrom1stCommit = getNumDaysBtw2Dates(firstCommitDate, futureCommitDate);


        String addedInCommitId = getCommitIdWhenFileWasAdded(fileName);
        int addedCommitCount = COMMIT_IDS.indexOf(addedInCommitId);
        Calendar addedCommitDate = COMMIT_IDS_WITH_DATES.get(addedInCommitId);
        long numOfDaysFrom1stCommitToWhenAdded = getNumDaysBtw2Dates(firstCommitDate, addedCommitDate);


        int countFromAddedToFuture = futureCommitCount - addedCommitCount;
        long numberOfDaysFromAddedToFuture = numOfDaysFrom1stCommit - numOfDaysFrom1stCommitToWhenAdded;

        if (numberOfDaysFromAddedToFuture - interval > 0 && countFromAddedToFuture > 2)
        {
            //get CommitId before interval / Metrics commit ID
            String metricsCommitId = getCommitIDBeforeNDays(futureCommitId, interval);
            Calendar metricsDate = COMMIT_IDS_WITH_DATES.get(metricsCommitId);
            int metricsCommitCount = COMMIT_IDS.indexOf(metricsCommitId);

            if (metricsDate.after(addedCommitDate) && !metricsCommitId.equals("") && metricsCommitCount > 1)
            {
                //get slopes
                slopesHistory = getFileSlopesForAllMetrics(fileName, addedInCommitId, metricsCommitId);
                metrics = getQualityMetricsForFileInCommit(fileName, metricsCommitId);

                //recent can be computed only if 11 commits, else history = recent
                if (metricsCommitCount - addedCommitCount > 10)
                {
                    int firstRecentCommitCount = metricsCommitCount - 10;
                    String firstRecentCommitId = COMMIT_IDS.get(firstRecentCommitCount);
                    slopesRecent = getFileSlopesForAllMetrics(fileName, firstRecentCommitId, metricsCommitId);

                }
                else
                {
                    slopesRecent = slopesHistory;
                }
            }

        }


        map.put("slopesHistory", slopesHistory);
        map.put("slopesRecent", slopesRecent);
        map.put("metrics", metrics);

        return map;

    }


    static String createFinalFileDataLine(String fileName, String futureCommitId, boolean isSmelly) throws IOException, ParseException
    {

        int[]intervalsInDays = {15, 30, 60, 90, 120};
        int i = 0;

        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();

        boolean considerThisFile = true;

        while (i < 5 && considerThisFile)
        {
            int interval = intervalsInDays[i];



            Map<String,ArrayList<Double>> slopes = getMetricsHistorySlopesRecentSlopesForFileInInterval(fileName, interval, futureCommitId);
            ArrayList<Double> slopesHistory = slopes.get("slopesHistory");
            ArrayList<Double> slopesRecent = slopes.get("slopesRecent");
            ArrayList<Double> metrics = slopes.get("metrics");

            //if first interval has no history, don't consider this file
            if (interval == 15 && slopesHistory.get(0) == -1.0 && slopesHistory.get(1) == -1.0 && slopesHistory.get(2) == -1.0 && slopesHistory.get(3) == -1.0 && slopesHistory.get(4) == -1.0 && slopesHistory.get(5) == -1.0 && slopesHistory.get(6) == -1.0)
            {
                considerThisFile = false;
            }

            else
            {

                for (Double sl : metrics) // 14 metrics
                {
                    allMetricsAndSlopesForAllIntervals.append(sl);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }

                for (Double m : slopesRecent) //14 slopes
                {
                    allMetricsAndSlopesForAllIntervals.append(m);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }


                for (Double sl : slopesHistory) // 14
                {
                    allMetricsAndSlopesForAllIntervals.append(sl);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }

                i++;
                System.out.println("interval: " + interval + " filename: " +  fileName + " futureCommitId: " + futureCommitId + " isSmelly: " + isSmelly);

                System.out.println("history: " + slopesHistory + " slopesRecent: " + slopesRecent + " metrics: " + metrics + " considerThisFile: " + considerThisFile);

            }

        }


        StringBuilder resultLine = new StringBuilder();
        if (considerThisFile)
        {
            resultLine.append(fileName).append(",")
                    .append(futureCommitId).append(",").append(allMetricsAndSlopesForAllIntervals);


            if (isSmelly)
            {
                ArrayList<String> currentSmells = new ArrayList<>();
                currentSmells = getSmellsForFileInCommit(fileName, futureCommitId);
                for (String smell : currentSmells) //3 smells
                {
                    resultLine.append(smell).append(",");
                }
            }

        }
        return resultLine.toString().replaceAll(",$", ""); //remove last comma

    }





    //remove commits in which the file was not present
    private static ArrayList<String> removeCommitsInWhichFileWasNotPresent(String fileName, ArrayList<String> commitsInBetweenHistory) throws IOException {
        ArrayList <String> commitsInWhichFileIsPresent = new ArrayList<>();
        for (String commitId : commitsInBetweenHistory)
        {
            if (getAllFileNamesInCommit(commitId).contains(fileName))
            {
                commitsInWhichFileIsPresent.add(commitId);
            }
        }
        return commitsInWhichFileIsPresent;
    }




    static LinkedHashMap <String, Calendar> getAllCommitIdsFromConsoleLines(List<String> linesFromConsole) throws IOException, ParseException {

        ArrayList<String> commitIds = new ArrayList<>();
        LinkedHashMap <String, Calendar> commitIdsWithDates = new LinkedHashMap<>();
        for (String line : linesFromConsole)
        {
            String commit = line.substring(0, 40);
            commitIds.add(commit);
            Calendar dateFromLine = readDateFromLine(line);
            commitIdsWithDates.put(commit,dateFromLine);
        }

        if (commitIds.isEmpty()) {
            log();
            System.err.print("No commits");
            System.exit(5);
        }

        return commitIdsWithDates;
    }


    private static ArrayList<Double> getSlopeForAllFileMetrics(ArrayList<ArrayList<Double>> metrics)
    {
        //Util.log();

        ArrayList<Double> slopes = new ArrayList<>();
        int xInterval = metrics.size();
        double[] xValues = new double[xInterval];

        for (int i = 0; i < xInterval; i++)
        {
            xValues[i] = ((double) i);
        }

        for (int j = 0; j < metrics.get(0).size(); j++) // 14 = metrics.get(commit).size()
        {
            double[] yValues = new double[xInterval]; //yValues will be created for each metric

            for (int k = 0; k < metrics.size(); k++) //for each commit
            {
                yValues[k] = (metrics.get(k).get(j)); //ad metric value to arraylist
            }
            slopes.add(getSlopeForFileMetric(xValues, yValues));
        }
        return slopes;
    }


    private static boolean isFilePresentInCommit(String fileName, String commitId) throws IOException
    {
        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line; //= br.readLine();
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(","); // use comma as separator
                if (cols[0].equals(fileName)) //found line with searched filename
                {
                    return true;
                }
            }
        }

        return false;
    }




    private static ArrayList<ArrayList<Double>> getQualityMetricsForFileInCommits(String fileName, ArrayList<String> commits) throws IOException
    {
        //Util.log();

        ArrayList<ArrayList<Double>> metricsForFile = new ArrayList<>();

        for (String commitId : commits)
        {
            // check if file is present in commit (because it may be deleted and then restored again)
            if (isFilePresentInCommit(fileName,commitId))
            {
                metricsForFile.add(getQualityMetricsForFileInCommit(fileName, commitId)); //add all metrics for fileName to ArrayList
            }
        }

        if (metricsForFile.size() == 0)
        {
            log();
            System.err.println("Error. metricsForFile = 0");
            System.exit(3);
        }

        return metricsForFile;
    }


    private static ArrayList<Double> getQualityMetricsForFileInCommit(String fileName, String commitId) throws IOException
    {
        ArrayList<Double> metrics = new ArrayList<>();

        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] cols = line.split(","); // use comma as separator
                if (cols[0].equals(fileName)) //found line with searched filename
                {
                    // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                    int i = 5; //quality metrics in csv file start from column 5
                    //System.out.println(cols.length);
                    while (i < cols.length) {
                        //System.out.println(cols[i]);
                        metrics.add(Double.parseDouble(cols[i]));
                        i++;
                    }
                }
            }
        }
        //
        if (metrics.size() == 0)
        {
            log();
            System.err.println("ERROR. For file " + fileName + " and commit ID " + commitId + " metrics = 0");
        }

        return metrics;

    }


    private static ArrayList<String> getSmellsForFileInCommit(String fileName, String commitId) throws IOException
    {
        ArrayList<String> smells = new ArrayList<>();
        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        br.readLine();
        String line;
        try
        {
            while ((line = br.readLine()) != null)
            {
                String[] cols = line.split(","); // use comma as separator
                if (cols[0].equals(fileName)) //found line with searched filename
                {
                // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                    int i = 2; //smells 1-4
                    //System.out.println(cols.length);
                    while ( i < 5) //end of smells column
                    {
                        //System.out.println(cols[i]);
                        smells.add((cols[i]));
                        i++;
                    }
                }
            }
        }
        finally
        {
            br.close();
        }
        return smells;

    }


    private static ArrayList getAllCommitsBetween(String pastCommitId, String currentCommitId)
    {
        ArrayList<String> commitsInBetween = new ArrayList<>();
        int indexPast = COMMIT_IDS.indexOf(pastCommitId);
        int indexCurrent = COMMIT_IDS.indexOf(currentCommitId);
        //System.out.println("IP= "+indexPast);
        //System.out.println("IC="+indexCurrent);
        for (int i = 0; i < COMMIT_IDS.size(); i++)
        {
            //System.out.println("i= " +i);
            if (i >= indexPast  && i < indexCurrent)
            {
                commitsInBetween.add(COMMIT_IDS.get(i));
            }
        }
        return commitsInBetween;
    }



    @SuppressWarnings("unchecked")
    private static ArrayList getAllFileNamesInCommit(String commitId) throws IOException
    {
        ArrayList fileNames = new ArrayList();
        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");

        //System.out.println("READING FILE: " + inputFile);

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");  // use comma as separator
                boolean added = fileNames.add(cols[0]);
                if (!added)
                {
                    log();
                    System.out.println("Error reading file names from the commit file");
                    System.exit(4);
                }
            }
        }
        return fileNames;
    }


    static void createOutputFileForEachCommit(ArrayList<String> commitIds) throws IOException, InterruptedException
    {
        ArrayList<String> linesFromConsoleOnCommitCheckout = new ArrayList<>();

        for (String commit : commitIds)
        {
            String pathToFileWithCommitSmells = OUTPUT_FOLDER_NAME.concat(commit).concat("-smells.csv");
            String pathFinalCommitResultFile = OUTPUT_FOLDER_NAME.concat(commit).concat(".csv");

            File finalCommitFile = new File(pathFinalCommitResultFile);
            File finalCommitFileWithSmells = new File(pathToFileWithCommitSmells);

            if (!finalCommitFile.exists() && !finalCommitFileWithSmells.exists())
            {
                //checkout each commit of the whole repository
                linesFromConsoleOnCommitCheckout.addAll(Git.executeCommandsAndReadLinesFromConsole(
                        PATH_TO_REPOSITORY, "git", "checkout", "-f", commit));

                //run pmd for each commit
                SmellDetector.runSmellDetector(pathToFileWithCommitSmells);

                HashMap<String, ArrayList<String>> fileNamesWithSmells = readSmellsFromFileToHashmap(pathToFileWithCommitSmells);

                Runner.computeQualityMetricsAndSmellsForCommitAndSaveToFile(pathFinalCommitResultFile, fileNamesWithSmells);

            }

        }

        for (String line: linesFromConsoleOnCommitCheckout)
        {
            writeLineToFile(line, FILE_PATH_TO_LINES_FROM_CONSOLE_ON_COMMITS_CHECKOUT);
        }
    }

//
//    private static int getCommitCountWhenFileWasRemoved(String fileName, int commitCount, ArrayList<String> commitIds) throws IOException {
//        //find first commit in which the file was not present and return the date of this commit
//        for (int i = commitCount + 1; i < commitIds.size(); i++)
//        {
//            ArrayList allFileNamesInCommit = getAllFileNamesInCommit(commitIds.get(i));
//            if (!allFileNamesInCommit.contains(fileName)) // current file is not present in commit (it means it was removed in this commit)
//            {
//                return i;
//            }
//        }
//        return -1; //0 when commit was never removed
//    }
//



}
