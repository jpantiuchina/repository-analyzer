package pipeline;

import com.github.mauricioaniche.ck.Runner;
import org.eclipse.jdt.internal.core.SourceType;

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
import static pipeline.Util.writeLineToFile;
import static pipeline.WholePipeline.*;
//import static pipeline.WholePipeline.OUTPUT_FOLDER_NAME;


class FileHandler
{

    static void handleFilesInCommits() throws IOException {
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


    private static void handleFileInCommit(String fileName, String commitId) throws IOException {
        ArrayList <String> smells = getSmellsForFileInCommit(fileName,commitId);
        if (smells.contains("true")) //save to smelly file
        {
            if(!SMELLY_FILE_NAMES.contains(fileName))
            {
                SMELLY_FILE_NAMES.add(fileName);
                // add to smelly result file
                String finalLine = createFinalFileDataLine(fileName, commitId, true);

                // add finalLine to output file
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
                writeLineToFile(finalLine, PATH_TO_CLEAN_FINAL_RESULT_FILE);
            }
        }

    }


    /* FORMAT
    "fileName, currentCommitId, currentCommitCount, currentNumOfDaysFromFirst, " + //for smelly
                   [ "isBlob, isCoupling, isNPath" +]
                    "addedInCommitId, addedInCommitCount, addedAfterDaysFromFirst" +
                    "CBO,      WMC,      DIT,      NOC,      RFC,      LCOM,      NOM,      NOPM,      NOSM,      NOF,      NOPF,      NOSF,      NOSI,      LOC," +
                    "CBOslopeAllHistory, WMCslopeAllHistory, DITslopeAllHistory, NOCslopeAllHistory, RFCslopeAllHistory, LCOMslopeAllHistory, NOMslopeAllHistory, NOPMslopeAllHistory, NOSMslopeAllHistory, NOFslopeAllHistory, NOPFslopeAllHistory, NOSFslopeAllHistory, NOSIslopeAllHistory, LOCslopeAllHistory, "
            );
     */

    private static String createFinalFileDataLine(String fileName, String currentCommitId, boolean isSmelly) throws IOException
    {
        StringBuilder resultLine = new StringBuilder();

        resultLine.append(fileName).append(",")
                .append(currentCommitId).append(",");

        int currentCommitCount = COMMIT_IDS.indexOf(currentCommitId);
        resultLine.append(currentCommitCount).append(",");

        String firstCommitId = COMMIT_IDS_WITH_DATES.keySet().iterator().next();
        Calendar firstCommitDate = COMMIT_IDS_WITH_DATES.get(firstCommitId);
        Calendar commitDate = COMMIT_IDS_WITH_DATES.get(currentCommitId);
        long numOfDaysFrom1stCommit = getNumDaysBtw2Dates(firstCommitDate,commitDate);
        resultLine.append(numOfDaysFrom1stCommit).append(",");


        // get when file was added to repo
        // initially we assume that it is a new file
        String addedInCommitId = currentCommitId;
        int addedCommitCount = currentCommitCount;
        long numOfDaysFrom1stCommitToWhenAdded = numOfDaysFrom1stCommit;
        Calendar addedCommitDate = COMMIT_IDS_WITH_DATES.get(addedInCommitId);

        boolean foundWhenAdded = false;
        ArrayList<String> allCommitsFromFirstToCurrent = new ArrayList<>();
        allCommitsFromFirstToCurrent = getAllCommitsBetween(firstCommitId, currentCommitId);
        allCommitsFromFirstToCurrent = removeCommitsInWhichFileWasNotPresent(fileName, allCommitsFromFirstToCurrent);
        if (allCommitsFromFirstToCurrent.size() > 0) {
            for (String addedCommitId : allCommitsFromFirstToCurrent) {
                if (!foundWhenAdded)
                {
                    ArrayList<String> fileNamesInCommit = getAllFileNamesInCommit(addedCommitId);
                    if (fileNamesInCommit.contains(fileName)) {
                        foundWhenAdded = true;
                        addedInCommitId = addedCommitId;
                        addedCommitCount = COMMIT_IDS.indexOf(addedCommitId);
                        addedCommitDate = COMMIT_IDS_WITH_DATES.get(addedCommitId);
                        numOfDaysFrom1stCommitToWhenAdded = getNumDaysBtw2Dates(firstCommitDate, addedCommitDate);
                    }
                }

            }
        }

        resultLine.append(addedInCommitId).append(",").append(addedCommitCount).append(",")
                .append(numOfDaysFrom1stCommitToWhenAdded).append(",");


      ArrayList<Double> metrics = getQualityMetricsForFileInCommit(fileName, currentCommitId);
      for (Double m : metrics) //14 metrics
      {
          resultLine.append(m);
          resultLine.append(",");
      }

        //get history to calculate slopes
        ArrayList<Double> slopesHistory;
        if (allCommitsFromFirstToCurrent.size() > 1) // to calculate the slope at least 2 commits needed
        {
            //retrieve all quality metrics for the specific file for each commit in the interval
            //System.out.println("Commits in between for file " +  fileName + commitsInBetweenHistory);
            ArrayList<ArrayList<Double>> metricsForFileHistory = getQualityMetricsForFileInCommits(fileName, allCommitsFromFirstToCurrent);
            //System.out.println("MetricsHistory for file " + fileName + " in commit " + commitID + metricsForFileHistory);
            slopesHistory = getSlopeForAllFileMetrics(metricsForFileHistory);
           // System.out.println("calculated slopes " + slopesHistory);
        }
        else
        {
            slopesHistory = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)); //if there are no history or one past commit, the slope can't be calculated
        }

        for (Double sl : slopesHistory) // 14 slopes
        {
            resultLine.append(sl);
            resultLine.append(",");
        }



        if (isSmelly)
        {
            ArrayList <String> currentSmells = new ArrayList<>();
            currentSmells =  getSmellsForFileInCommit(fileName, currentCommitId);
            for (String smell : currentSmells) //3 smells
            {
                resultLine.append(smell).append(",");
            }
        }

        //If file is not smelly then simply the interval from added to current
        int countFromAddedToCurrent = currentCommitCount - addedCommitCount;
        long numberOfDaysFromAddedToCurrent = getNumDaysBtw2Dates(addedCommitDate, commitDate);

        resultLine.append(countFromAddedToCurrent).append(",");
        resultLine.append(numberOfDaysFromAddedToCurrent).append(",");


        return resultLine.toString().replaceAll(",$", ""); //remove last comma

    }





//
//    /*TODO params, RETURN
//    Returns final result line consisting from:
//    fileName, commitId, Commit#from1st, NumDaysFrom1st, WillBeRemovedInCommit, WillBeRemovedAfterDays, [metrics], [slopesAll], [slopes10], [smells]
// */
//    @SuppressWarnings("unchecked")
//    static void handleFileInCommit(String fileName, String commitID, int commitCount, LinkedHashMap<String, Calendar> commitIdsWithDates,
//                                   HashMap<String, HashMap<String,ArrayList<String>>> commitIdsWithFileSmells, String resultFilePath) throws IOException
//    {
//        ArrayList<String> commitIds = new ArrayList<>(commitIdsWithDates.keySet());
//
//        String firstCommitId = commitIdsWithDates.keySet().iterator().next();
//        Calendar firstCommitDate = commitIdsWithDates.get(firstCommitId);
//        Calendar commitDate = commitIdsWithDates.get(commitID);
//
//        long numOfDaysFrom1stCommit = getNumDaysBtw2Dates(firstCommitDate,commitDate);
//        long removedAfterDays = -1;
//        String removedInCommitId = "-1";
//
//        int removedInCommitCount = getCommitCountWhenFileWasRemoved(fileName, commitCount, commitIds);
//        if (removedInCommitCount != -1) //file was removed
//        {
//            removedInCommitId = commitIds.get(removedInCommitCount);
//            Calendar removedInCommitDate = commitIdsWithDates.get(removedInCommitId);
//            removedAfterDays = getNumDaysBtw2Dates(commitDate,removedInCommitDate);
//            System.out.println("File: " + fileName + " was removed after " + removedAfterDays + " days, which is after " + removedInCommitCount + " commits.");
//        }
//
//        ArrayList<Double> metrics = getQualityMetricsForFileInCommit(fileName, commitID); //14 quality metrics
//
//        ArrayList<String> commitsInBetweenHistory = getAllCommitsBetween(firstCommitId, commitID);
//        commitsInBetweenHistory = removeCommitsInWhichFileWasNotPresent(fileName,commitsInBetweenHistory);
//        System.out.println("commitsInBetweenHistory=" + commitsInBetweenHistory);
//
//        ArrayList<Double> slopesHistory  = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)); //if there are no history or one past commit, the slope can't be calculated
//
//        if (commitsInBetweenHistory.size() > 1) // to calculate the slope at least 2 commits needed
//        {
//            //retrieve all quality metrics for the specific file for each commit in the interval
//            System.out.println("Commits in between for file " +  fileName + commitsInBetweenHistory);
//            ArrayList<ArrayList<Double>> metricsForFileHistory = getQualityMetricsForFileInCommits(fileName, commitsInBetweenHistory);
//            System.out.println("MetricsHistory for file " + fileName + " in commit " + commitID + metricsForFileHistory);
//            slopesHistory = getSlopeForAllFileMetrics(metricsForFileHistory);
//            System.out.println("calculated slopes " + slopesHistory);
//
//        }
//
//
//        //recent trend
////        ArrayList<Double> slopesRecent = new ArrayList<>(Arrays.asList(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)); //if there are no 10 recen't slopes, the slope can't be calculated
////        if (commitCount > 10)
////        {
////            String firstRecentCommit = commitIds.get(commitCount-10);
////
////            ArrayList<String> commitsInBetweenRecent = getAllCommitsBetween(firstRecentCommit, commitID, commitIds);
////            commitsInBetweenRecent = removeCommitsInWhichFileWasNotPresent(fileName,commitsInBetweenRecent);
////            if (commitsInBetweenRecent.size() > 1)
////            {
////                ArrayList<ArrayList<Double>> metricsForFileRecent = getQualityMetricsForFileInCommits(fileName, commitsInBetweenRecent);
////                slopesRecent = getSlopeForAllFileMetrics(metricsForFileRecent);
////            }
////
////        }
//
//        // 3 + smells
//        ArrayList<String> smells = commitIdsWithFileSmells.get(commitID).get(fileName); //null if no smells for this file
//
//        //if it is not smelly, then report: after how many days the file became smelly, in which coommitCount and in which commitID
//        int becameSmellyInCommitCount = -1;
//        String becameSmellyInCommitId = "-1";
//        long becameSmellyAfterDays = -1;
//
//        //ArrayList <String> noSmells = new ArrayList<>(Arrays.asList("false","false","false","false"));
//
//        String becameBlobIn = null;
//        String becameCoupling = null;
//        String becameNPath = null;
//
//        if (smells == null) //file is not smelly
//        {
//          //  smells = noSmells;
//
//            becameSmellyInCommitCount = commitCount + 1; //get next commit
//            while (becameSmellyInCommitCount < commitIds.size() && commitIdsWithFileSmells.get(commitIds.get(becameSmellyInCommitCount)).get(fileName) == null) //
//            {
//                becameSmellyInCommitCount++;
//            }
//            if (becameSmellyInCommitCount < commitIds.size()) //becameSmellyInCommitCount was found
//            {
//                becameSmellyInCommitId = commitIds.get(becameSmellyInCommitCount);
//                Calendar becameSmellyInCommitDate = commitIdsWithDates.get(becameSmellyInCommitId);
//                becameSmellyAfterDays = getNumDaysBtw2Dates(commitDate,becameSmellyInCommitDate);
//
//                ArrayList <String> becameSmells;
//                becameSmells = getSmellsForFileInCommit(fileName,becameSmellyInCommitId);
//                becameBlobIn = becameSmells.get(1);
//                becameCoupling = becameSmells.get(2);
//                becameNPath = becameSmells.get(3);
//
//
//            }
//            else // file never became smelly
//                becameSmellyInCommitCount = -1;
//
//        }
//
//
//
//       ArrayList <String> currentSmells;
//        currentSmells = getSmellsForFileInCommit(fileName,commitID);
//
//
//        System.out.println("Adding line: ");
////        System.out.println(fileName + "\n" + commitID + "\n" +  commitCount + "\n" +  numOfDaysFrom1stCommit+ "\n" +  removedInCommitCount+ "\n" +
////                removedInCommitId+ "\n" +  removedAfterDays+ "\n" +  metrics+ "\n" +  slopesHistory+ "\n" +
////                slopesRecent+ "\n" +  currentSmells + "\n" +  resultFilePath+ "\n" +  becameSmellyInCommitCount+ "\n" +  becameSmellyInCommitId+ "\n" +  becameSmellyAfterDays );
////
////        addFinalFileData(fileName, commitID, commitCount, numOfDaysFrom1stCommit, removedInCommitCount, removedInCommitId, removedAfterDays, metrics, slopesHistory,
////                slopesRecent, currentSmells, resultFilePath, becameSmellyInCommitCount, becameSmellyInCommitId, becameSmellyAfterDays, becameBlobIn, becameCoupling, becameNPath );
//
//    }

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






//    private static void addFinalFileData(String fileName, String currentCommitId, int commitCount, long numOfDaysFrom1stCommit, int removedInCommitCount,
//                                         String removedInCommitId, long removedAfterDays, ArrayList<Double> metrics, ArrayList<Double> slopesHistory,
//                                         ArrayList<Double> slopesRecent, ArrayList<String> smells, String resultFilePath,
//                                         int becameSmellyInCommitCount, String becameSmellyInCommitId, long becameSmellyAfterDays,
//                                         String becameBlobIn, String becameCoupling, String becameNPath) throws IOException
//    {
//        StringBuilder resultLine = new StringBuilder();
//
//        resultLine.append(fileName).append(",")
//                .append(currentCommitId).append(",")
//                .append(commitCount).append(",")
//                .append(numOfDaysFrom1stCommit).append(",")
//                .append(removedInCommitCount).append(",")
//                .append(removedInCommitId).append(",")
//                .append(removedAfterDays).append(",")
//                .append(becameSmellyInCommitCount).append(",")
//                .append(becameSmellyInCommitId).append(",")
//                .append(becameSmellyAfterDays).append(",")
//                .append(becameBlobIn).append(",")
//                .append(becameCoupling).append(",")
//                .append(becameNPath).append(",")
//        ;
//
//
//         //14 quality metrics
//        for (Double m : metrics)
//        {
//            resultLine.append(m);
//            resultLine.append(",");
//        }
//
//        for (Double sl : slopesHistory) //14 slopes
//        {
//            resultLine.append(sl);
//            resultLine.append(",");
//        }
//
//        for (Double sl : slopesRecent) //14 slopes
//        {
//            resultLine.append(sl);
//            resultLine.append(",");
//        }
//
//
//        for (String s : smells) //4 smells
//        {
//            resultLine.append(s);
//            resultLine.append(",");
//        }
//
//        //remove last comma and add resultLine to output file
//        writeLineToFile(resultLine.toString().replaceAll(",$", ""),resultFilePath);
//    }


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
