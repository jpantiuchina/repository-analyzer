package pipeline;

import org.eclipse.jdt.internal.core.SourceType;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static pipeline.FileData.*;
import static pipeline.FileHandler.getAllFilesInFolder;
import static pipeline.Git.executeCommandsAndReadLinesFromConsole;
import static pipeline.Git.readDateFromLine;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;
import static pipeline.Util.getDateBeforeOrAfterNDays;
import static pipeline.Util.log;
import static pipeline.Util.writeLineToFile;
import static pipeline.WholePipeline.*;

class FileData
{

    static class FileCommitData
    {
        boolean isFileSmelly = false;

        double loc;
        double lcom;
        double wmc;// = 0;
        double rfc;// = 0;
        double cbo;// = 0;
        double nom;// = 0;
        double noa;// = 0;
        double dit;// = 0;
        double noc;// = 0;

        Calendar time;

        String commitId = "";

        int commitCount = 0;

        FileCommitData(boolean isFileSmelly, String commitId, int commitCount, Calendar time,
                       double loc, double lcom, double wmc, double rfc, double cbo, double nom, double noa, double dit, double noc)
        {
        this.commitId = commitId;
        this.commitCount = commitCount;
        this.time = time;
        this.loc = loc;
        this.lcom = lcom;
        this.wmc = wmc;
        this.rfc = rfc;
        this.cbo = cbo;
        this.nom = nom;
        this.noa = noa;
        this.dit = dit;
        this.noc = noc;
        this.isFileSmelly = isFileSmelly;
        }
    }



    String fileNamePath = "";
    boolean becomeSmelly = false;
    boolean isBlob = false;
    boolean isCDSBP = false;
    boolean isComplexClass = false;
    boolean isFuncDec = false;
    boolean isSpaghCode = false;
    ArrayList fileCommitDataArrayList = new ArrayList();

    FileData(String fileNamePath, boolean becomeSmelly, boolean isBlob, boolean isCDSBP,
             boolean isComplexClass, boolean isFuncDec, boolean isSpaghCode, ArrayList<FileCommitData> fileCommitDataArrayList)
    {
        this.fileNamePath = fileNamePath;
        this.becomeSmelly = becomeSmelly;
        this.isBlob = isBlob;
        this.isCDSBP = isCDSBP;
        this.isComplexClass = isComplexClass;
        this.isFuncDec = isFuncDec;
        this.isSpaghCode = isSpaghCode;
        this.fileCommitDataArrayList = fileCommitDataArrayList;
    }

    static boolean isFileSmelly(String fileData)
    {
        return fileData.contains("true");
    }

    static boolean isBlob(String smellType)
    {
        return smellType.contains("blob");
    }

    static boolean isCDSBP(String smellType)
    {
        return smellType.contains("cdsbp");
    }

    static boolean isComplexClass(String smellType)
    {
        return smellType.contains("complexClass");
    }

    static boolean isFuncDec(String smellType)
    {
        return smellType.contains("fd");
    }

    static boolean isSpaghCode(String smellType)
    {
        return smellType.contains("spaghettiCode");
    }

}


class FileHandler
{

    static void handleAllFiles(File[] allFiles) throws IOException, ParseException {
        if (allFiles != null)
        {
            for (File file : allFiles)
            {
                if (file.isFile())
                {
                  //  System.out.println(file);
                    handleFileAndAddFinalFileDataLines(file);
                }
            }
        }
    }

    static void addCommitFilesForEverySmellyFile() throws IOException, InterruptedException {
        File[] allFilesInFolder = getAllFilesInFolder("result/");

        for (File folderFilePath : allFilesInFolder)
        {
            if (folderFilePath.toString().contains(REPO_FOLDER_NAME) &&folderFilePath.isFile() && folderFilePath.toString().contains("-smelly.csv")) //for every smelly folderFileName
            {
                try (BufferedReader br = new BufferedReader(new FileReader(folderFilePath)))
                {
                    String folderFileName = folderFilePath.toString().replaceAll("-smelly.csv$","");
                    String cleanFolderFileName = folderFileName.concat("-clean.csv");

                    //System.out.println("folderFilePath: " + folderFilePath);
                    String line = br.readLine();

                    while ((line = br.readLine()) != null) //read lines from clean files
                    {
                        //System.out.println("LIne: " + line);

                        String[] cols = line.split(","); // use comma as separator

                        String smellyFileName = cols[0];
                        String commitId = cols[1];
                        //System.out.println("COMMITID: " + commitId);

                        ArrayList<String> fileNamesInCommit = getAllFileNamesPresentInCommitId(commitId);

                        for (String fileName : fileNamesInCommit)
                        {

                            if (!fileName.equals(smellyFileName) && !isFileAndCommitIdPresentInCleanFile(cleanFolderFileName, fileName, commitId)) // add fileName and CommitId data to clean file
                            {
                                int commitIdCount = COMMIT_IDS.indexOf(commitId);
                                while (!isFileAndCommitIdPresentInCleanFile(cleanFolderFileName, fileName, COMMIT_IDS.get(commitIdCount)) && commitIdCount > 0)
                                {
                                    commitIdCount--;
                                }

                                //check if found commitId when file was last time edited
                                String foundCommitId = COMMIT_IDS.get(commitIdCount);
                                if (isFileAndCommitIdPresentInCleanFile(cleanFolderFileName, fileName, foundCommitId))
                                {
                                    String newDataLineWithReplacedCommitId = readDataLineFromFileForFileAndCommitIdAndReplaceCommitId(cleanFolderFileName, fileName, foundCommitId, commitId);
                                    if(!newDataLineWithReplacedCommitId.isEmpty())
                                    {
                                        writeLineToFile(newDataLineWithReplacedCommitId, cleanFolderFileName);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    private static String readDataLineFromFileForFileAndCommitIdAndReplaceCommitId(String cleanFilePath, String fileName, String commitIdToFind, String newCommitId ) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(cleanFilePath)))
        {
            String line = br.readLine();

            while ((line = br.readLine()) != null)
            {
                String[] cols = line.split(","); // use comma as separator

                String fileNameInFile = cols[0];
                String commitIdInFile = cols[1];

                if (fileNameInFile.equals(fileName) && commitIdInFile.equals(commitIdToFind))
                {
                    cols[1] = newCommitId;

                    String newLine = Arrays.toString(cols).replace("[","");
                    newLine = newLine.replace("]","");
                    return newLine;
                }
            }
        }

        return "";
    }


    private static boolean isFileAndCommitIdPresentInCleanFile(String cleanFilePath, String fileName, String commitId) throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(cleanFilePath)))
        {
            String line = br.readLine();

            while ((line = br.readLine()) != null)
            {
                String[] cols = line.split(","); // use comma as separator

                String fileNameInFile = cols[0];
                String commitIdInFile = cols[1];

                if (fileNameInFile.equals(fileName) && commitIdInFile.equals(commitId))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static String makeFileName(String fileName)
    {
        String newName = fileName;
        //System.out.println("-> " + fileName);
        Pattern pattern = Pattern.compile("\\w*\\.\\w*$");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find())
        {
            newName = matcher.group(0);
            //System.out.println(newName);
        }

        return newName;
    }

    private static ArrayList<String> getAllFileNamesPresentInCommitId(String commitId) throws IOException, InterruptedException {
        ArrayList<String> output = executeCommandsAndReadLinesFromConsole(new File(REPO_FOLDER_NAME), "git", "show", "--name-only", "--oneline", commitId);
        //System.out.println(output);

        ArrayList<String> fileNames = new ArrayList<>();
        for (String fileName: output)
        {
            if (fileName.contains(".java"))
            {
                fileName = fileName.replaceAll("\\.java$", "");
                fileName = fileName.replaceAll("\\/", "." );

                fileName = makeFileName(fileName);
                fileNames.add(fileName);
               // System.out.println("Added file:" + fileName);
            }
        }

        return fileNames;

    }

    //private static boolean isFileAndCommitIdPresentInCleanFile






    private static String getCommitIdWhenFileBecomeSmelly(FileData fd) throws IOException {

        String commitId = "";

        ArrayList<FileData.FileCommitData> fileCommitData = fd.fileCommitDataArrayList;

        for (FileData.FileCommitData fc : fileCommitData)
        {
            if (fc.isFileSmelly && commitId.equals("")) //get first time
            {
                commitId = fc.commitId;
            }
        }
        return commitId;
    }


    private static FileData parseFileWithItsData(String filePath) throws IOException {

        String fileData = filePath.replaceAll("^.*\\/", ""); //removes path
        String fileName = fileData.replaceAll("(_.*)", ""); //gets filename
        fileName = makeFileName(fileName);
        //System.out.println("->>"+fileName);

        String smellType = "";
        Pattern pattern = Pattern.compile("(_\\w*?_)");
        Matcher matcher = pattern.matcher(fileData);
        if (matcher.find())
        {
            smellType = matcher.group(1);
        }

        return new FileData(fileName, isFileSmelly(fileData), isBlob(smellType), isCDSBP(smellType),
                isComplexClass(smellType), isFuncDec(smellType), isSpaghCode(smellType), readAllDataInFileAndGetArraylistOfFileCommitDataObjects(filePath)
                );
    }



    private static ArrayList<FileData.FileCommitData> readAllDataInFileAndGetArraylistOfFileCommitDataObjects(String inputFile) throws IOException {

        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
        {
            String line;
            int count = 1;
            while ((line = br.readLine()) != null)
            {
              //  System.out.println(count + " LINE: " + line);
                String[] cols = line.split(";"); // use comma as separator
               // System.out.println("COLS1 " + cols[1]);
                boolean isFileSmelly = parseBoolean(cols[1]);
                double loc = parseDouble(cols[2]);
                double lcom = parseDouble(cols[3]);
                double wmc = parseDouble(cols[4]);
                double rfc = parseDouble(cols[5]);
                double cbo = parseDouble(cols[6]);
                double nom = parseDouble(cols[7]);
                double noa = parseDouble(cols[8]);
                double dit = parseDouble(cols[9]);
                double noc = parseDouble(cols[10]);
                String commitId = cols[(cols.length-1)];
                Calendar commitTime = COMMIT_IDS_WITH_DATES.get(commitId);

                FileData.FileCommitData fileCommitData = new FileData.FileCommitData
                        (isFileSmelly, commitId, count, commitTime, loc, lcom, wmc, rfc, cbo, nom, noa, dit, noc);

                fileCommitDataArrayList.add(fileCommitData);
                count++;
            }
        }
        return fileCommitDataArrayList;
    }




    static File[] getAllFilesInFolder(String folder)
    {
        File[] files = new File(folder).listFiles();

        for (File file : files)
        {
            System.out.println(file.toString());
        }

        return files;
    }

    private static ArrayList<Double> getFileSlopesForAllMetrics(FileData fd, String beginCommitIncluding, String lastCommitExcluding) throws IOException
    {
        ArrayList<String> slopeCommits = getAllCommitsBetween(fd, beginCommitIncluding, lastCommitExcluding);

        ArrayList<Double> slopes = null;

        if (slopeCommits.size() > 1) // to calculate the slope at least 2 commits needed
        {

            ArrayList<ArrayList<Double>> metricsForCommitsInBetween = new ArrayList<ArrayList<Double>>();

            for (String slopeCommit: slopeCommits)
            {
                metricsForCommitsInBetween.add(getMetricsForCommit(fd, slopeCommit));
            }

            slopes = getSlopeForAllFileMetrics(metricsForCommitsInBetween);
        }
        else
        {
            log();
            System.out.println("Error, wrong number of slope commits");


            System.out.println("CommitInBTW: " + slopeCommits);
            System.out.println("BCommitId: " + beginCommitIncluding);
            System.out.println("EndCommitId" + lastCommitExcluding);

            System.exit(6);
        }

        return slopes;
    }





    private static void handleFileAndAddFinalFileDataLines(File filePath) throws IOException, ParseException
    {
        FileData fd = parseFileWithItsData(filePath.toString());
        String futureCommitId = "";

        if (fd.becomeSmelly) //smelly file, read only one line when file became smelly
        {
            SMELLY_FILE_NAMES.add(fd.fileNamePath);
            String commitIdWhenFileBecameSmelly = getCommitIdWhenFileBecomeSmelly(fd);

            if (commitIdWhenFileBecameSmelly.isEmpty()) {
                log();
                System.out.println("File Specified as smelly, but no smelly commitID found");
                System.exit(4);
            }
            futureCommitId = commitIdWhenFileBecameSmelly;

            String finalLine = createFinalLine(fd, futureCommitId);

            // add finalLine to output file
            if (!finalLine.isEmpty())
                writeLineToFile(finalLine, PATH_TO_SMELLY_FINAL_RESULT_FILE);

        } else if (!SMELLY_FILE_NAMES.contains(fd.fileNamePath)) //clean file, process every line
        {
            ArrayList<FileData.FileCommitData> fileCommitData = fd.fileCommitDataArrayList;

            for (FileData.FileCommitData fc : fileCommitData)
            {
                String finalLine = createFinalLine(fd, fc.commitId);

                // add finalLine to output file
                if (!finalLine.isEmpty())
                    writeLineToFile(finalLine, PATH_TO_CLEAN_FINAL_RESULT_FILE);
            }

        }
    }


    private static String getMetricsCommitId(FileData fd, String commitId, int interval) throws ParseException
    {
        Calendar timeFutureCommit = COMMIT_IDS_WITH_DATES.get(commitId);
        Calendar dateBeforeNDays = getDateBeforeOrAfterNDays(timeFutureCommit, -interval);

        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        for (FileCommitData fileCommitData : fileCommitDataArrayList)
        {
           String metricsCommitId = fileCommitData.commitId;
           Calendar metricsCommitTime = fileCommitData.time;
           int count = fileCommitData.commitCount;

           if (metricsCommitTime.after(dateBeforeNDays) && timeFutureCommit.after(metricsCommitTime) && count > 2)
           {
               return metricsCommitId;
           }
        }
       return "";
    }

    private static String getRecentCommitId(FileData fd, String metricsCommitId)
    {

        String recentCommitId = "";
        int metricsCommitCount = getCountOfCommitId(fd, metricsCommitId);
        if (metricsCommitCount <= 10 && metricsCommitCount != 0)
        {
            recentCommitId = metricsCommitId;
        }
        else if (metricsCommitCount > 10)
        {
            ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

            FileCommitData fileCommitData = fileCommitDataArrayList.get(metricsCommitCount - 10);

            recentCommitId = fileCommitData.commitId;
        }
        else
            {
                log();
                System.out.println("Error finding recent commit id");
                System.exit(6);
            }


        return recentCommitId;

    }

    private static int getCountOfCommitId(FileData fd, String commitId)
    {
        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        int count = 0;
        for (FileCommitData fileCommitData : fileCommitDataArrayList)
        {
            if (fileCommitData.commitId.equals(commitId))
                count = fileCommitData.commitCount;
        }
        return count;
    }

    private static String getFirstCommitId(FileData fd)
    {
        FileCommitData firstFileCommitData = (FileCommitData) fd.fileCommitDataArrayList.get(0);
        return firstFileCommitData.commitId;
    }

    private static ArrayList<Double> getMetricsForCommit(FileData fd, String commitId) throws IOException
    {
        FileCommitData fileCommitDataForMetrics = getCommitDataForFileFromCommit(fd, commitId);

        ArrayList<Double> metrics = new ArrayList<>(Arrays.asList(fileCommitDataForMetrics.loc, fileCommitDataForMetrics.lcom,
                fileCommitDataForMetrics.wmc, fileCommitDataForMetrics.rfc, fileCommitDataForMetrics.cbo, fileCommitDataForMetrics.nom,
                fileCommitDataForMetrics.noa, fileCommitDataForMetrics.dit, fileCommitDataForMetrics.noc));

        return metrics;
    }


    private static String createFinalLine(FileData fd, String futureCommitId) throws IOException, ParseException {

        int days = 15;

        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();

        while (days <= 120)
        {
            String metricsCommitId = getMetricsCommitId(fd, futureCommitId, days); //can return "" empty string

            if (days == 15 && metricsCommitId.isEmpty())
            {
                return "";
            }
            else if (metricsCommitId.isEmpty() && days > 15) // fill with -
            {
                System.out.println("==============");
                System.out.println("found days: " + days);
                int count  = 0;
                while (count < 27)
                {
                    allMetricsAndSlopesForAllIntervals.append("-").append(",");
                    count++;
                }
            }
            else //slopes can be calculated
            {

                String recentCommitId = getRecentCommitId(fd, metricsCommitId);

                ArrayList<Double> metrics = getMetricsForCommit(fd, metricsCommitId);

                ArrayList<Double> slopesHistory = getFileSlopesForAllMetrics(fd, getFirstCommitId(fd), metricsCommitId);

                ArrayList<Double> slopesRecent;
                if (recentCommitId.equals(metricsCommitId)) {
                    slopesRecent = slopesHistory;
                } else {
                    slopesRecent = getFileSlopesForAllMetrics(fd, recentCommitId, metricsCommitId);
                }



                /* loc, lcom, wmc, rfc, cbo, nom, noa, dit, noc */

                for (Double sl : metrics) // 9 metrics
                {
                    allMetricsAndSlopesForAllIntervals.append(sl);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }

                for (Double m : slopesRecent) //9 slopes
                {
                    allMetricsAndSlopesForAllIntervals.append(m);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }


                for (Double sl : slopesHistory) // 9
                {
                    allMetricsAndSlopesForAllIntervals.append(sl);
                    allMetricsAndSlopesForAllIntervals.append(",");
                }
            }


            days += 15; //15,30,45,60,75,90,105,120
        }


        StringBuilder resultLine = new StringBuilder();

            resultLine.append(fd.fileNamePath).append(",")
                    .append(futureCommitId).append(",").append(allMetricsAndSlopesForAllIntervals);


            if (fd.becomeSmelly)
            {
                ArrayList<String> currentSmells = new ArrayList<>();
                currentSmells = getSmellsForFileInCommit(fd); //
                for (String smell : currentSmells) //3 smells
                {
                    resultLine.append(smell).append(",");
                }
            }

        return resultLine.toString().replaceAll(",$", ""); //remove last comma

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

        for (int j = 0; j < metrics.get(0).size(); j++) // 10 = metrics.get(commit).size()
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


    private static ArrayList<FileCommitData> getCommitDataForFileInCommits(FileData fd, ArrayList<String> commits) throws IOException
    {
        ArrayList<FileCommitData> fileCommitData = new ArrayList<>();

        for (String commitId : commits)
        {
            fileCommitData.add(getCommitDataForFileFromCommit(fd, commitId));
        }

        if (fileCommitData.size() == 0)
        {
            log();
            System.err.println("Error. metricsForFile = 0");
            System.exit(3);
        }

        return fileCommitData;
    }


    private static FileCommitData getCommitDataForFileFromCommit(FileData fd, String commitId) throws IOException
    {
        ArrayList<FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        for (FileCommitData fileCommitData : fileCommitDataArrayList)
        {
            if (fileCommitData.commitId.equals(commitId))
            {
                return fileCommitData;
            }
        }

        log();
        System.err.println("ERROR. For file " + fd.toString() + " and commit ID " + commitId + " metrics = 0");
        System.exit(6);

        return null;
    }



    private static ArrayList<String> getSmellsForFileInCommit(FileData fd) throws IOException
    {
        ArrayList<String> smells = new ArrayList<>();

        String isBlob = String.valueOf(fd.isBlob);
        String isCDSBP = String.valueOf(fd.isCDSBP);
        String isComplexClass = String.valueOf(fd.isComplexClass);
        String isSpaghetti = String.valueOf(fd.isSpaghCode);
        String isFuncDec = String.valueOf(fd.isFuncDec);

        smells.add(isBlob); smells.add(isCDSBP); smells.add(isComplexClass); smells.add(isFuncDec); smells.add(isSpaghetti);

        return smells;

    }


    private static String getCommitIdFromCount(FileData fd, int count)
    {
        ArrayList<FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;
        return fileCommitDataArrayList.get(count).commitId;
    }


    private static ArrayList<String> getAllCommitsBetween(FileData fd, String pastCommitId, String currentCommitId)
    {
        ArrayList<String> commitsInBetween = new ArrayList<>();
        int indexPast = getCountOfCommitId(fd, pastCommitId);
        int indexCurrent =getCountOfCommitId(fd, currentCommitId);


        for (int i = indexPast; i < indexCurrent; i++)
        {
            commitsInBetween.add(getCommitIdFromCount(fd, i));
        }

        return commitsInBetween;
    }


}
