package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static pipeline.JavaFile.*;
import static pipeline.Git.executeCommandsAndReadLinesFromConsole;
import static pipeline.Git.readDateFromLine;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;
import static pipeline.Util.*;
import static pipeline.WholePipeline.*;


@SuppressWarnings("unchecked")
class FileHandler
{

    static void handleAllCleanFiles(File[] allFiles) throws IOException, ParseException
    {
        if (allFiles != null)
        {
            for (File file : allFiles)
            {
                if (file.isFile())
                {
                    handleFileAndAddFinalFileDataLines(file);
                }
            }
        }
    }

    private static ArrayList<String> getOnlyCleanFileNames(ArrayList<String> allFileNames)
    {
        //System.out.println(SMELLY_CSV_FILE_NAMES);
        ArrayList<String> cleanFileNames = new ArrayList<>();
        for (String fileName : allFileNames)
        {
            if (!SMELLY_CSV_FILE_NAMES.contains(fileName))
            {
                cleanFileNames.add(fileName);
            }
        }
        return cleanFileNames;
    }


    private static HashMap<JavaFile, String> getCommitIdOfPreviousEdit(String fileName, String currentCommitId)
    {
        HashMap<JavaFile, String> fileDataAndCommitId = new HashMap<>();
        int count = COMMIT_IDS.indexOf(currentCommitId);

        JavaFile javaFile = allFilesData.get(0);

        for (JavaFile fd : allFilesData)
        {
            if (fd.fileNamePath.equals(fileName))
            {
                ArrayList<Commit> fileCommitDataList = fd.fileCommitArrayList;

                for (int i = fileCommitDataList.size() - 1; i >= 0; i--)
                {
                    String commitId = fileCommitDataList.get(i).commitId;
                    int commitCount = COMMIT_IDS.indexOf(commitId);
                    if (commitCount < count) //commit which was before
                    {
                        fileDataAndCommitId.put(fd, commitId);
                        return fileDataAndCommitId;
                    }
                }
                fileDataAndCommitId.put(javaFile, currentCommitId);
                return fileDataAndCommitId; //not found, first commit
            }
        }

        // these files were present in commit when running git show, but were not present in the folder with the received data
        //        System.out.println("Not found!");
        //        System.out.println(fileName);
        //        System.out.println(currentCommitId);
        return null;
    }

    static void addCommitFilesForEverySmellyFile() throws IOException, InterruptedException, ParseException
    {
        try (BufferedReader br = new BufferedReader(new FileReader(PATH_TO_SMELLY_FINAL_RESULT_FILE)))
        {
            @SuppressWarnings("UnusedAssignment") String line = br.readLine(); //skip headerLine

            while ((line = br.readLine()) != null) //read lines from smelly files
            {
                String[] cols = line.split(","); // use comma as separator

                String futureCommitIdSmelly = cols[1];

                //to get the system's snapshots, we need all smelly files in final cleanFolder file except smelly files

                ArrayList<String> allFileNamesInCommit = getAllFileNamesPresentInCommitId(futureCommitIdSmelly); //git show will return all smelly and clean files
  //              System.out.println(allFileNamesInCommit);
                //check if file is not present in the list of smelly file names
                ArrayList<String> cleanFileNames = getOnlyCleanFileNames(allFileNamesInCommit);

                if (!cleanFileNames.isEmpty())
                {
                    for (String cleanFileName : cleanFileNames)
                    {
                        if (getCommitIdOfPreviousEdit(cleanFileName, futureCommitIdSmelly) != null)
                        {
                            HashMap<JavaFile, String> fileDataAndCommitIdOfPreviousEdit = getCommitIdOfPreviousEdit(cleanFileName, futureCommitIdSmelly);

                            assert fileDataAndCommitIdOfPreviousEdit != null;
                            JavaFile fd = fileDataAndCommitIdOfPreviousEdit.keySet().iterator().next();
                            String commitId = fileDataAndCommitIdOfPreviousEdit.get(fd);

                            composeLineAndWriteToFile(fd, commitId, futureCommitIdSmelly, PATH_TO_CLEAN_FINAL_RESULT_FILE);
                        }
                    }
                }
            }
        }
    }


    private static String getFileNameAsInFileButNotGitShow(String fileNameInGitShow)
    {

        for (String cleanFile :CLEAN_CSV_FILE_NAMES)
        {
            if (cleanFile.contains(fileNameInGitShow) || fileNameInGitShow.contains(cleanFile))
            {
                return cleanFile;
            }
        }

        for (String smellyFile: SMELLY_CSV_FILE_NAMES)
        {
            if (smellyFile.contains(fileNameInGitShow) || fileNameInGitShow.contains(smellyFile))
            {
                return smellyFile;
            }
        }

        return "";
    }


    private static String makeFileNameFromAbsolutePath(String fileName)
    {
        return fileName.replaceAll("^.*.java.", "");
    }

    private static ArrayList<String> getAllFileNamesPresentInCommitId(String commitId) throws IOException, InterruptedException
    {
        ArrayList<String> output = executeCommandsAndReadLinesFromConsole(new File(REPO_FOLDER_NAME), "git", "show", "--name-only", "--oneline", commitId);
        //System.out.println(output);

        ArrayList<String> fileNames = new ArrayList<>();
        for (String fileName : output)
        {
            if (fileName.contains(".java"))
            {
                fileName = fileName.replaceAll("\\.java$", "");
                fileName = fileName.replaceAll("/", ".");

                // System.out.println(fileName);
                fileName = makeFileNameFromAbsolutePath(fileName);

                fileName = getFileNameAsInFileButNotGitShow(fileName);
                if (!fileName.isEmpty())
                {
                    fileNames.add(fileName);

                }

                //System.out.println("Added file:" + fileName);
            }
        }

        return fileNames;

    }


    private static String getCommitIdWhenFileBecomeSmelly(JavaFile fd) throws IOException
    {

        String commitId = "";

        ArrayList<Commit> fileCommitData = fd.fileCommitArrayList;

        for (Commit fc : fileCommitData)
        {
            if (fc.isFileSmelly && commitId.equals("")) //get first time
            {
                commitId = fc.commitId;
            }
        }

        if (commitId.isEmpty())
        {
            log();
            System.out.println("File Specified as smelly, but no smelly commitID found");
            System.exit(4);
        }

        return commitId;
    }


    private static JavaFile parseFileWithItsData(String filePath) throws IOException
    {

        String fileData = filePath.replaceAll("^.*/", ""); //removes path
        String fileName = fileData.replaceAll("(_.*)", ""); //gets filename

        String smellType = "";
        Pattern pattern = Pattern.compile("(_\\w*?_)");
        Matcher matcher = pattern.matcher(fileData);
        if (matcher.find())
        {
            smellType = matcher.group(1).replaceAll("_", "");
        }

        return new JavaFile(fileName, isFileSmelly(fileData), isBlob(smellType), isCDSBP(smellType), isComplexClass(smellType), isFuncDec(smellType), isSpaghCode(smellType), readAllDataInFileAndGetArraylistOfFileCommitDataObjects(filePath));
    }


    private static ArrayList<Commit> readAllDataInFileAndGetArraylistOfFileCommitDataObjects(String inputFile) throws IOException
    {

        ArrayList<Commit> fileCommitDataArrayList = new ArrayList<>();
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
                String commitId = cols[(cols.length - 1)];

                Calendar commitTime = COMMIT_IDS_WITH_DATES.get(commitId);

                // some commits were not found when running git log, therefore reading time from file
                if (commitTime == null)
                {
                    commitTime = GregorianCalendar.getInstance();
                    commitTime.setTimeInMillis(parseLong(cols[(cols.length - 2)]) * 1000);
                    COMMIT_IDS_WITH_DATES.put(commitId, commitTime);
                }

                Commit fileCommitData = new Commit(isFileSmelly, commitId, count, commitTime, loc, lcom, wmc, rfc, cbo, nom, noa, dit, noc);

                fileCommitDataArrayList.add(fileCommitData);
                count++;
            }
        }
        return fileCommitDataArrayList;
    }


    static File[] getAllFilesInFolder(String folder)
    {
        return new File(folder).listFiles();
    }

    private static ArrayList<Double> getFileSlopesForMetricsBetween2Commits(JavaFile fd, Commit beginFileDataIncluding, Commit lastCommitDataExcluding) throws IOException
    {

        ArrayList<Commit> slopeCommitsData = getAllCommitsDataInBetween(fd, beginFileDataIncluding, lastCommitDataExcluding);

        ArrayList<Double> slopes = null;

        if (slopeCommitsData.size() > 1) // to calculate the slope at least 2 commits needed
        {

            ArrayList<ArrayList<Double>> metricsForCommitsInBetween = new ArrayList<>();

            for (Commit slopeCommitData : slopeCommitsData)
            {
                metricsForCommitsInBetween.add(getMetricsOfFileCommitData(slopeCommitData));
            }

            slopes = getSlopeForAllFileMetrics(metricsForCommitsInBetween);
        } else
        {
            log();
            System.out.println("Error, wrong number of slope commits");

            System.out.println("BCommitId: " + beginFileDataIncluding.commitId);
            System.out.println("EndCommitId" + lastCommitDataExcluding.commitId);

            System.exit(6);
        }

        return slopes;
    }


    private static void handleFileAndAddFinalFileDataLines(File filePath) throws IOException, ParseException
    {
        JavaFile fd = parseFileWithItsData(filePath.toString());

        allFilesData.add(fd);

        //System.out.println(fd.fileNamePath);

        if (fd.becomeSmelly) //smelly file, read only one line when file became smelly
        {
            if (!SMELLY_CSV_FILE_NAMES.contains(fd.fileNamePath))
            {
                SMELLY_CSV_FILE_NAMES.add(fd.fileNamePath);
            }
            String futureCommitIdWhenFileBecameSmelly = getCommitIdWhenFileBecomeSmelly(fd);


            composeLineAndWriteToFile(fd, futureCommitIdWhenFileBecameSmelly, "", PATH_TO_SMELLY_FINAL_RESULT_FILE);
        }
        else
        {
            CLEAN_CSV_FILE_NAMES.add(fd.fileNamePath);
        }

    }

    private static String makeUniqueString(String commitId, String futureCommitId, String substitudeFutureCommitId)
    {
        String uniqueStr;
        if (!substitudeFutureCommitId.isEmpty())
        {
            uniqueStr = commitId.concat(substitudeFutureCommitId);
        } else
        {
            uniqueStr = commitId.concat(futureCommitId);
        }
        return uniqueStr;
    }


    private static void composeLineAndWriteToFile(JavaFile fd, String futureCommitId, String subsituteFutureCommitId, String outputFilePath) throws IOException, ParseException
    {

        String uniqueString = makeUniqueString(fd.fileNamePath, futureCommitId, subsituteFutureCommitId);

        if (!uniquePairs.contains(uniqueString) || fd.becomeSmelly)  //add all for smelly
        {
            String lineNumericalData = createFinalLineWithSlopesData(fd, futureCommitId);

            if (!lineNumericalData.isEmpty())
            {
                if (!CREATED)
                {
                    createEmptyFinalResultFiles();

                }
                if (!subsituteFutureCommitId.isEmpty())
                {
                    futureCommitId = subsituteFutureCommitId;
                }
                String finalLine = String.valueOf(new StringBuilder(fd.fileNamePath).append(",").
                        append(futureCommitId).append(",").
                        append(lineNumericalData));

                CREATED = true;

                writeLineToFile(finalLine, outputFilePath);
            }
            uniquePairs.add(uniqueString);
        }

    }


    private static Commit getMetricsCommitData(JavaFile fd, String futureCommitId, int interval) throws ParseException
    {
        Calendar timeFutureCommit = COMMIT_IDS_WITH_DATES.get(futureCommitId);
        //System.out.println(timeFutureCommit);
        Calendar dateBeforeNDays = getDateBeforeOrAfterNDays(timeFutureCommit, -interval);

        int futureCommitCount = COMMIT_IDS.indexOf(futureCommitId);
        int commitCountBeforeNInterval = futureCommitCount - interval;


        ArrayList<Commit> fileCommitDataArrayList = fd.fileCommitArrayList;

        int i = fileCommitDataArrayList.size();

        while (i > 0 && commitCountBeforeNInterval > 2)
        {
            i--;
            Commit fileCommitData = fileCommitDataArrayList.get(i);
            Calendar metricsCommitTime = fileCommitData.time;
            int count = fileCommitData.commitCount;


            if (IS_PREDICTION_IN_DAYS)
            {
                if (metricsCommitTime.before(dateBeforeNDays) && timeFutureCommit.after(metricsCommitTime) && count > 2)
                {
                    return fileCommitData;
                }
            } else //in commits
            {
                if (count < commitCountBeforeNInterval && timeFutureCommit.after(metricsCommitTime) && count > 2)
                {
                    return fileCommitData;
                }
            }

        }
        return null;
    }

    private static Commit getRecentFileCommitData(JavaFile fd, Commit metricsFileCommitData)
    {
        int recentCommitCount = 0;
        ArrayList<Commit> fileCommitDataArrayList = fd.fileCommitArrayList;
        int metricsCommitCount = getCountOfFileCommitData(fd, metricsFileCommitData);
        if (2 <= metricsCommitCount && metricsCommitCount < 10)
        {
            recentCommitCount = 0;
        } else if (metricsCommitCount >= 10)
        {

            recentCommitCount = metricsCommitCount - 10;
        } else
        {
            Util.log();
            System.err.println("Error finding recent commit id");
            System.err.println(metricsCommitCount);
            System.exit(6);
        }

        return fileCommitDataArrayList.get(recentCommitCount);

    }

    private static int getCountOfFileCommitData(JavaFile fd, Commit fileCommitData)
    {
        ArrayList<Commit> fileCommitDataArrayList = fd.fileCommitArrayList;
        return fileCommitDataArrayList.indexOf(fileCommitData);
    }


    private static ArrayList<Double> getMetricsOfFileCommitData(Commit fileCommitData) throws IOException
    {
        @SuppressWarnings("UnnecessaryLocalVariable") ArrayList<Double> metrics = new ArrayList<>(Arrays.asList(fileCommitData.loc, fileCommitData.lcom, fileCommitData.wmc, fileCommitData.rfc, fileCommitData.cbo, fileCommitData.nom, fileCommitData.noa, fileCommitData.dit, fileCommitData.noc));

        return metrics;
    }


    private static String createFinalLineWithSlopesData(JavaFile fd, String futureCommitId) throws IOException, ParseException
    {

        int currentCount = STEP;

        String metricsCommitIdforInterval;

        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();
        StringBuilder resultLine = new StringBuilder();

        while (currentCount <= MAX_INT_DAYS_OR_COMMITS)
        {
            Commit metricsFileCommitData = getMetricsCommitData(fd, futureCommitId, currentCount);

            if (metricsFileCommitData == null && currentCount == STEP) // metrics commit not found for the smallest interval
            {
                return "";
            } else if (metricsFileCommitData == null && currentCount > STEP) //there were slopes for at least one interval
            {
                int count = 0;
                while (count < 9 * 3 + 1)
                {
                    allMetricsAndSlopesForAllIntervals.append("-").append(",");
                    count++;
                }
            } else if (metricsFileCommitData != null)//slopes can be calculated for this interval
            {
                Commit recentFileCommitData = getRecentFileCommitData(fd, metricsFileCommitData);

                ArrayList<Double> metrics = getMetricsOfFileCommitData(metricsFileCommitData);

                Commit firstFileCommitData = (Commit) fd.fileCommitArrayList.get(0);

                ArrayList<Double> slopesHistory = getFileSlopesForMetricsBetween2Commits(fd, firstFileCommitData, metricsFileCommitData);


                ArrayList<Double> slopesRecent;
                if (!firstFileCommitData.equals(recentFileCommitData))
                {
                    slopesRecent = getFileSlopesForMetricsBetween2Commits(fd, recentFileCommitData, metricsFileCommitData);
                } else
                {
                    slopesRecent = slopesHistory;
                }


                metricsCommitIdforInterval = metricsFileCommitData.commitId;
                allMetricsAndSlopesForAllIntervals.append(metricsCommitIdforInterval).append(",");

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


                //                System.out.println("Interval: " + currentCount);
                //                System.out.println(allMetricsAndSlopesForAllIntervals);
                //                System.out.println();
            } else
            {
                Util.log();
                System.out.println("ERROR, metricsFileCommitData == null ");
                System.exit(4);
            }


            currentCount = currentCount + STEP; //15,30,45,60,75,90,105,120
        }

        resultLine.append(allMetricsAndSlopesForAllIntervals);


        if (fd.becomeSmelly)
        {
            ArrayList<String> currentSmells = getSmellsForFileInCommit(fd); //
            for (String smell : currentSmells) //5 smells
            {
                resultLine.append(smell).append(",");
            }
        }

        return resultLine.toString().replaceAll(",$", ""); //remove last comma

    }

    static LinkedHashMap<String, Calendar> getAllCommitIdsFromConsoleLines(List<String> linesFromConsole) throws IOException, ParseException
    {

        ArrayList<String> commitIds = new ArrayList<>();
        LinkedHashMap<String, Calendar> commitIdsWithDates = new LinkedHashMap<>();
        for (String line : linesFromConsole)
        {
            String commit = line.substring(0, 40);
            commitIds.add(commit);
            Calendar dateFromLine = readDateFromLine(line);
            commitIdsWithDates.put(commit, dateFromLine);
        }

        if (commitIds.isEmpty())
        {
            log();
            System.err.print("No commits");
            System.exit(5);
        }

        return commitIdsWithDates;
    }


    private static ArrayList<Double> getSlopeForAllFileMetrics(ArrayList<ArrayList<Double>> metrics)
    {
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


    private static ArrayList<String> getSmellsForFileInCommit(JavaFile fd) throws IOException
    {
        ArrayList<String> smells = new ArrayList<>();

        String isBlob = String.valueOf(fd.isBlob);
        String isCDSBP = String.valueOf(fd.isCDSBP);
        String isComplexClass = String.valueOf(fd.isComplexClass);
        String isSpaghetti = String.valueOf(fd.isSpaghCode);
        String isFuncDec = String.valueOf(fd.isFuncDec);

        smells.add(isBlob);
        smells.add(isCDSBP);
        smells.add(isComplexClass);
        smells.add(isFuncDec);
        smells.add(isSpaghetti);

        return smells;

    }


    private static ArrayList<Commit> getAllCommitsDataInBetween(JavaFile fd, Commit pastFileCommitData, Commit currentFileCommitData)
    {
        ArrayList<Commit> commitsDataInBetween = new ArrayList<>();
        int indexPast = getCountOfFileCommitData(fd, pastFileCommitData);
        int indexCurrent = getCountOfFileCommitData(fd, currentFileCommitData);

        for (int i = indexPast; i < indexCurrent; i++)
        {
            ArrayList<Commit> fileCommitDataArrayList = fd.fileCommitArrayList;
            commitsDataInBetween.add(fileCommitDataArrayList.get(i));
        }
        return commitsDataInBetween;
    }


}
