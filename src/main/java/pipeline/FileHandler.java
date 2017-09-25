package pipeline;

import com.sun.xml.internal.bind.v2.TODO;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import static pipeline.Git.getNumDaysBtw2Dates;
import static pipeline.Git.readDateFromLine;
import static pipeline.ResultFileWriter.*;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;


class FileHandler
{
    static ArrayList<String> commits = new ArrayList<>();

    static int getCommitCountWhenFileWasRemoved(String fileName, int commitCount, ArrayList<String> commitIds) throws IOException {
        //find first commit in which the file was not present and return the date of this commit
        for (int i = commitCount + 1; i < commitIds.size(); i++)
        {
            ArrayList<String> allFileNamesInCommit = getAllFileNamesInCommit(commitIds.get(i));
            if (!allFileNamesInCommit.contains(fileName)) // current file is not present in commit (it means it was removed in this commit)
            {
                return i;
            }
        }
        return -1; //0 when commit was never removed
    }

    /*TODO params, RETURN
    Returns final result line consisting from:
    fileName, commitId, Commit#from1st, NumDaysFrom1st, WillBeRemovedInCommit, WillBeRemovedAfterDays, [metrics], [slopesAll], [slopes10], [smells]
 */
    static void handleFileInCommit(String fileName, String commitID, int commitCount, LinkedHashMap<String, Calendar> commitIdsWithDates,
                                   HashMap<String, HashMap<String,ArrayList<String>>> commitIdsWithFileSmells, String resultFilePath) throws IOException {
        ArrayList<String> commitIds = new ArrayList<String>(commitIdsWithDates.keySet());

        String firstCommitId = commitIdsWithDates.keySet().iterator().next();
        Calendar firstCommitDate = commitIdsWithDates.get(firstCommitId);
        Calendar commitDate = commitIdsWithDates.get(commitID);

        long numOfDaysFrom1stCommit = getNumDaysBtw2Dates(firstCommitDate,commitDate);
        long removedAfterDays = -1;
        String removedInCommitId = "-1";

        int removedInCommitCount = getCommitCountWhenFileWasRemoved(fileName, commitCount, commitIds);
        if (removedInCommitCount != -1) //file was removed
        {
            removedInCommitId = commitIds.get(removedInCommitCount);
            Calendar removedInCommitDate = commitIdsWithDates.get(removedInCommitId);
            removedAfterDays = getNumDaysBtw2Dates(commitDate,removedInCommitDate);
            System.out.println("File: " + fileName + " was removed after " + removedAfterDays + " days, which is after " + removedInCommitCount + " commits.");
        }

        ArrayList<Double> metrics = getQualityMetricsForFileInCommit(fileName, commitID); //14 quality metrics

        ArrayList<String> commitsInBetweenHistory = getAllCommitsBetween(firstCommitId, commitID, commitIds);
        commitsInBetweenHistory = removeCommitsInWhichFileWasNotPresent(fileName,commitsInBetweenHistory);
        System.out.println("commitsInBetweenHistory=" + commitsInBetweenHistory);

        ArrayList<Double> slopesHistory  = new ArrayList<Double>(Arrays.asList(-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0 )); //if there are no history or one past commit, the slope can't be calculated

        if (commitsInBetweenHistory.size() > 1) // to calculate the slope at least 2 commits needed
        {
            //retrieve all quality metrics for the specific file for each commit in the interval
            System.out.println("Commits in between for file " +  fileName + commitsInBetweenHistory);
            ArrayList<ArrayList<Double>> metricsForFileHistory = getQualityMetricsForFileInCommits(fileName, commitsInBetweenHistory);
            System.out.println("MetricsHistory for file " + fileName + " in commit " + commitID + metricsForFileHistory);
            slopesHistory = getSlopeForAllFileMetrics(metricsForFileHistory);
            System.out.println("calculated slopes " + slopesHistory);

        }


        //recent trend
        ArrayList<Double> slopesRecent = new ArrayList<Double>(Arrays.asList(-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0 )); //if there are no 10 recen't slopes, the slope can't be calculated
        if (commitCount > 10)
        {
            String firstRecentCommit = commitIds.get(commitCount-10);

            ArrayList<String> commitsInBetweenRecent = getAllCommitsBetween(firstRecentCommit, commitID, commitIds);
            commitsInBetweenRecent = removeCommitsInWhichFileWasNotPresent(fileName,commitsInBetweenRecent);
            if (commitsInBetweenRecent.size() > 1)
            {
                ArrayList<ArrayList<Double>> metricsForFileRecent = getQualityMetricsForFileInCommits(fileName, commitsInBetweenRecent);
                slopesRecent = getSlopeForAllFileMetrics(metricsForFileRecent);
            }

        }

        // 3 + smells
        ArrayList<String> smells = commitIdsWithFileSmells.get(commitID).get(fileName); //null if no smells for this file

        //if it is not smelly, then report: after how many days the file became smelly, in which coommitCount and in which commitID
        int becameSmellyInCommitCount = -1;
        String becameSmellyInCommitId = "-1";
        long becameSmellyAfterDays = -1;

        ArrayList <String> noSmells = new ArrayList<>(Arrays.asList("false","false","false","false"));


        if (smells == null) //file is not smelly
        {
            smells = noSmells;

            becameSmellyInCommitCount = commitCount + 1; //get next commit
            while (becameSmellyInCommitCount < commitIds.size() && commitIdsWithFileSmells.get(commitIds.get(becameSmellyInCommitCount)).get(fileName) == null) //
            {
                becameSmellyInCommitCount++;
            }
            if (becameSmellyInCommitCount < commitIds.size()) //becameSmellyInCommitCount was found
            {
                becameSmellyInCommitId = commitIds.get(becameSmellyInCommitCount);
                Calendar becameSmellyInCommitDate = commitIdsWithDates.get(becameSmellyInCommitId);
                becameSmellyAfterDays = getNumDaysBtw2Dates(commitDate,becameSmellyInCommitDate);
            }
            else // file never became smelly
                becameSmellyInCommitCount = -1;

        }



       ArrayList <String> currentSmells;
        currentSmells = getSmellsForFileInCommit(fileName,commitID);


        System.out.println("Adding line: ");
        System.out.println(fileName + "\n" + commitID + "\n" +  commitCount + "\n" +  numOfDaysFrom1stCommit+ "\n" +  removedInCommitCount+ "\n" +
                removedInCommitId+ "\n" +  removedAfterDays+ "\n" +  metrics+ "\n" +  slopesHistory+ "\n" +
                slopesRecent+ "\n" +  currentSmells + "\n" +  resultFilePath+ "\n" +  becameSmellyInCommitCount+ "\n" +  becameSmellyInCommitId+ "\n" +  becameSmellyAfterDays );

        addFinalFileData(fileName, commitID, commitCount, numOfDaysFrom1stCommit, removedInCommitCount, removedInCommitId, removedAfterDays, metrics, slopesHistory,
                slopesRecent, currentSmells, resultFilePath, becameSmellyInCommitCount, becameSmellyInCommitId, becameSmellyAfterDays );

    }

    //remove commits in which the file was not present
    static ArrayList<String> removeCommitsInWhichFileWasNotPresent(String fileName, ArrayList<String> commitsInBetweenHistory) throws IOException {
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



    static String createEmptyFinalResultFile(String clonedRepoFolderName) throws IOException
    {
        //removes previous result file
        File result = new File("result");
        String resultFilePath = ((result.toString()).concat("/").concat(clonedRepoFolderName).concat(".csv"));
        if (result.mkdir())
        {
            System.out.print(result.toString() + " folder created. ");
        }
        else
        {
            System.out.print(result.toString() + " folder was not created because it already exist. ");
        }

        System.out.println("Results will be saved (or overwritten) to file " + resultFilePath);

        PrintStream ps = new PrintStream(resultFilePath);//TODO
        ps.println("fileName, currentCommitId, commitCountFrom1st, numOfDaysFrom1stCommit, removedInCommitCount, " +
                "removedInCommitId, RemovedAfterDays, " +
                "becameSmellyInCommitCount, becameSmellyInCommitId, becameSmellyAfterDays, " +
                "CBO,      WMC,      DIT,      NOC,      RFC,      LCOM,      NOM,      NOPM,      NOSM,      NOF,      NOPF,      NOSF,      NOSI,      LOC," +
                "CBOslopeAllHistory, WMCslopeAllHistory, DITslopeAllHistory, NOCslopeAllHistory, RFCslopeAllHistory, LCOMslopeAllHistory, NOMslopeAllHistory, NOPMslopeAllHistory, NOSMslopeAllHistory, NOFslopeAllHistory, NOPFslopeAllHistory, NOSFslopeAllHistory, NOSIslopeAllHistory, LOCslopeAllHistory, " +
                "CBOslope10Recent, WMCslope10Recent, DITslope10Recent, NOCslope10Recent, RFCslope10Recent, LCOMslope10Recent, NOMslope10Recent, NOPMslope10Recent, NOSMslope10Recent, NOFslope10Recent, NOPFslope10Recent, NOSFslope10Recent, NOSIslope10Recent, LOCslope10Recent, " +
                "isSmelly,         isBlob,         isCoupling ,         isNPath"
        );
        ps.close();

        return resultFilePath;
    }


    static void addFinalFileData(String fileName, String currentCommitId, int commitCount, long numOfDaysFrom1stCommit, int removedInCommitCount,
                                 String removedInCommitId, long removedAfterDays, ArrayList<Double> metrics,  ArrayList<Double> slopesHistory,
                                 ArrayList<Double> slopesRecent, ArrayList<String> smells, String resultFilePath,
                                 int becameSmellyInCommitCount, String becameSmellyInCommitId, long becameSmellyAfterDays) throws IOException {
        StringBuilder resultLine = new StringBuilder();

        resultLine.append(fileName).append(",")
                .append(currentCommitId).append(",")
                .append(commitCount).append(",")
                .append(numOfDaysFrom1stCommit).append(",")
                .append(removedInCommitCount).append(",")
                .append(removedInCommitId).append(",")
                .append(removedAfterDays).append(",")
                .append(becameSmellyInCommitCount).append(",")
                .append(becameSmellyInCommitId).append(",")
                .append(becameSmellyAfterDays).append(",");


         //14 quality metrics
        for (Double m : metrics)
        {
            resultLine.append(m);
            resultLine.append(",");
        }

        for (Double sl : slopesHistory) //14 slopes
        {
            resultLine.append(sl);
            resultLine.append(",");
        }

        for (Double sl : slopesRecent) //14 slopes
        {
            resultLine.append(sl);
            resultLine.append(",");
        }


        for (String s : smells) //4 smells
        {
            resultLine.append(s);
            resultLine.append(",");
        }

        //remove last comma and add resultLine to output file
        writeLineToFile(resultLine.toString().replaceAll(",$", ""),resultFilePath);
    }


    static LinkedHashMap <String, Calendar> getAllCommitIdsFromConsoleLines(List<String> linesFromConsole) throws IOException, ParseException {

        ArrayList<String> commitIds = new ArrayList<>();
        ArrayList<Calendar> dates = new ArrayList<>();
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


     static void saveCommitIdsToFile(ArrayList<String> commitIds, String outputFilePath) throws IOException, ParseException
    {
        removeFileIfPresent(outputFilePath);

       // System.out.println();

            for (String commit : commitIds)
            {
            //    System.out.print(commit);
            //    System.out.print(' ');
                writeLineToFile(commit, outputFilePath);
            }
      //  System.out.println();
    }





    static ArrayList<Double> getSlopeForAllFileMetrics(ArrayList<ArrayList<Double>> metrics)
    {
        ArrayList<Double> slopes = new ArrayList<>();
        int xInterval = metrics.size();

        double[] xValues = new double[xInterval];

        for (int i = 0; i < xInterval; i++)
        {
            xValues[i] = ((double) i);
        }

            for (int j = 0; j < 14; j++) // 14 = metrics.get(commit).size()
            {
                double[] yValues = new double[xInterval]; //yValues will be created for each metric

                for (int k = 0; k < metrics.size(); k++) //for each commit
                {
                    yValues[k] = (metrics.get(k).get(j)); //ad metric value to arraylist
                }

                slopes.add(getSlopeForFileMetric(xValues, yValues));

                System.out.println(slopes);
            }
        return slopes;
    }

    static ArrayList<ArrayList<Double>> getQualityMetricsForFileInCommits(String fileName, ArrayList<String> commits) throws IOException
    {
        //stores commitId as a key, ArrayList of metric values
        ArrayList<ArrayList<Double>> metricsForFile = new ArrayList<>();


        System.out.println("CommitSize: " + commits.size());
        for (int i = 0; i < commits.size(); i++)
        {
            String commitId = commits.get(i);
            System.out.println("CommitId: " + commitId);
            metricsForFile.add(getQualityMetricsForFileInCommit(fileName, commitId)); //add all metrics for fileName to hashmap
        }

        return metricsForFile;
    }

    static ArrayList<Double> getQualityMetricsForFileInCommit(String fileName, String commitId) throws IOException
    {
        ArrayList<Double> metrics = new ArrayList<>();

        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(","); // use comma as separator
            if (cols[0].equals(fileName)) //found line with searched filename
            {
               // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                int i = 5; //quality metrics in csv file start from column 5
                //System.out.println(cols.length);
                while ( i < cols.length)
                {
                    //System.out.println(cols[i]);
                    metrics.add(Double.parseDouble(cols[i]));
                    i++;
                }
            }
        }
        System.out.println("getQualityMetricsForFileInCommit filename: " + fileName + " commitId " + commitId +  " metrics: " + metrics) ;
        return metrics;

    }


    static ArrayList<String> getSmellsForFileInCommit(String fileName, String commitId) throws IOException {
        ArrayList<String> smells = new ArrayList<>();

        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(","); // use comma as separator
            if (cols[0].equals(fileName)) //found line with searched filename
            {
                // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                int i = 1; //smells 1-4
                //System.out.println(cols.length);
                while ( i < 5) //end of smells column
                {
                    //System.out.println(cols[i]);
                    smells.add((cols[i]));
                    i++;
                }
            }
        }
        return smells;

    }


    static ArrayList getAllCommitsBetween(String pastCommitId, String currentCommitId, ArrayList<String> allCommits)
    {
        ArrayList<String> commitsInBetween = new ArrayList<>();
        int indexPast = allCommits.indexOf(pastCommitId);
        int indexCurrent = allCommits.indexOf(currentCommitId);
        //System.out.println("IP= "+indexPast);
        //System.out.println("IC="+indexCurrent);
        for (int i = 0; i < allCommits.size(); i++)
        {
            //System.out.println("i= " +i);
            if (i >= indexPast  && i < indexCurrent)
            {
                commitsInBetween.add(allCommits.get(i));
            }
        }

        System.out.println("Commits betweem: " + pastCommitId + " and " + currentCommitId + " : " + commitsInBetween);
        return commitsInBetween;
    }



    static ArrayList getAllFileNamesInCommit(String commitId) throws IOException
    {
        ArrayList fileNames = new ArrayList();
        File inputFile = new File(OUTPUT_FOLDER_NAME + commitId + ".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(",");
            // use comma as separator
            fileNames.add(cols[0]);
            //System.err.println("FILENAME: " + cols[0]);
        }
        br.close();
        return fileNames;
    }


    static ArrayList getIntersectionOfFilesInCommits(ArrayList<String> pastFiles, ArrayList<String> currentFiles, ArrayList<String> futureFiles)
    {
        ArrayList commonFiles = new ArrayList();
        for (String pastFile : pastFiles)
        {
            for (String currentFile : currentFiles)
            {
                for (String futureFile : futureFiles)
                {
                    if (pastFile.equals(currentFile) && pastFile.equals(futureFile))
                    {
                        commonFiles.add(futureFile);
                    }
                }
            }
        }
        return  commonFiles;
    }


    private static List<List<String>> removeEmptyGroups(List<List<String>> groups)
    {
        for (int i = groups.size() - 1; i >= 0; i--)
        {
            if (groups.get(i).isEmpty())
            {
                groups.remove(i);
            }
        }
        return groups;
    }


}
