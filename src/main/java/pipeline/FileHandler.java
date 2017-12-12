package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static pipeline.FileData.*;
import static pipeline.Git.executeCommandsAndReadLinesFromConsole;
import static pipeline.Git.readDateFromLine;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;
import static pipeline.Util.*;
import static pipeline.WholePipeline.*;


@SuppressWarnings("unchecked")
class FileHandler
{

    static void handleAllFilesForRepo(File[] allFiles) throws IOException, ParseException
    {
        if (allFiles != null)
        {
            for (File file : allFiles)
            {
                if (file.isFile())
                {

                        handleFileAndAddFinalFileDataLines(file);



                    //handleFileAndAddFinalFileDataLines(file);
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


    private static HashMap<FileData, String> getCommitIdOfPreviousEdit(String fileName, String currentCommitId)
    {
        HashMap<FileData, String> fileDataAndCommitId = new HashMap<>();
        int count = COMMIT_IDS.indexOf(currentCommitId);

        for (FileData fd : allFilesData)
        {
            if (fd.fileNamePath.equals(fileName))
            {
                ArrayList<FileCommitData> fileCommitDataList = fd.fileCommitDataArrayList;

                for (int i = fileCommitDataList.size() - 1; i >= 0; i--)
                {
                    String commitId = fileCommitDataList.get(i).commitId;
                    int commitCount = COMMIT_IDS.indexOf(commitId);
                    if (commitCount <= count) //commit which was before
                    {
                        fileDataAndCommitId.put(fd, commitId);
                        return fileDataAndCommitId;
                    }
                }
                return null; //not found, first commit
            }
        }

        // these files were present in commit when running git show, but were not present in the folder with the received data
        //        System.out.println("Not found!");
        //        System.out.println(fileName);
        //        System.out.println(currentCommitId);
        return null;
    }


    private static int getTotalNumOfSteps(int totalCols)
    {
        return (totalCols - 7) / 30;
    }

    static void addCleanFilesForEverySmellyFileInCommit() throws IOException, InterruptedException, ParseException
    {
        try (BufferedReader br = new BufferedReader(new FileReader(PATH_TO_SMELLY_FINAL_RESULT_FILE)))
        {
            @SuppressWarnings("UnusedAssignment") String line = br.readLine(); //skip headerLine

            while ((line = br.readLine()) != null) //read lines from smelly files
            {
                //System.out.println(line);
                String[] cols = line.split(","); // use comma as separator

                int totalCols = cols.length;

                int totalNumberOfSteps = getTotalNumOfSteps(totalCols);
                int currentStep = 1;
                while (currentStep <= totalNumberOfSteps)
                {

                    //int metricsCommitIdColumnForStep = (currentStep) + 2 + 29 * (currentStep - 1) - 1;
                    int metricsCommitIdColumnForStep = 30 * currentStep - 28;
                    String metricsCommitId = cols[metricsCommitIdColumnForStep];
                    //to get the system's snapshots, we need all smelly files in final cleanFolder file except smelly files
                    if (!metricsCommitId.equals("-"))
                    {
                        ArrayList<String> allFileNamesInCommit = getAllFileNamesPresentInCommitId(metricsCommitId); //git show will return all smelly and clean files
                        // System.out.println(allFileNamesInCommit);
                        //check if file is not present in the list of smelly file names
                        ArrayList<String> cleanFileNames = getOnlyCleanFileNames(allFileNamesInCommit);

                        if (!cleanFileNames.isEmpty())
                        {
                            for (String cleanFileName : cleanFileNames)
                            {
                                HashMap<FileData, String> fileDataAndCommitIdOfPreviousEdit = getCommitIdOfPreviousEdit(cleanFileName, metricsCommitId);
                                if (fileDataAndCommitIdOfPreviousEdit != null)
                                {
                                    FileData fd = fileDataAndCommitIdOfPreviousEdit.keySet().iterator().next();
                                    String commitId = fileDataAndCommitIdOfPreviousEdit.get(fd);

                                    ArrayList<FileCommitData> fcd = fd.fileCommitDataArrayList;
                                    for (FileCommitData commit : fcd)
                                    {
                                        if (commit.commitId.equals(commitId) && commit.commitCount > 2)
                                        {
                                            composeLineAndWriteToCleanFile(fd, commitId, metricsCommitId, currentStep, PATH_TO_CLEAN_FINAL_RESULT_FILE);

                                        }
                                    }
                                }
                            }
                        }
                    }
                    currentStep++;

                }
            }
        }
    }


    private static String getFileNameAsInFileButNotGitShow(String fileNameInGitShow)
    {

        for (String cleanFile : CLEAN_CSV_FILE_NAMES)
        {
            if (cleanFile.contains(fileNameInGitShow) || fileNameInGitShow.contains(cleanFile))
            {
                return cleanFile;
            }
        }

        for (String smellyFile : SMELLY_CSV_FILE_NAMES)
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


    private static String getCommitIdWhenFileBecomeSmelly(FileData fd) throws IOException
    {

        String commitId = "";

        ArrayList<FileData.FileCommitData> fileCommitData = fd.fileCommitDataArrayList;

        for (FileData.FileCommitData fc : fileCommitData)
        {
            if (fc.isFileSmelly) //get first commit id when file becomes smelly
            {
                return fc.commitId;
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


    private static FileData parseFileWithItsData(String filePath) throws IOException
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

        return new FileData(fileName, isFileSmelly(fileData), isBlob(smellType), isCDSBP(smellType), isComplexClass(smellType), isFuncDec(smellType), isSpaghCode(smellType), readAllDataInFileAndGetArraylistOfFileCommitDataObjects(filePath));
    }


    private static ArrayList<FileData.FileCommitData> readAllDataInFileAndGetArraylistOfFileCommitDataObjects(String inputFile) throws IOException
    {

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
                String commitId = cols[(cols.length - 1)];

                Calendar commitTime = COMMIT_IDS_WITH_DATES.get(commitId);

                // some commits were not found when running git log, therefore reading time from file
                if (commitTime == null)
                {
                    commitTime = GregorianCalendar.getInstance();
                    commitTime.setTimeInMillis(parseLong(cols[(cols.length - 2)]) * 1000);
                    COMMIT_IDS_WITH_DATES.put(commitId, commitTime);
                }

                FileData.FileCommitData fileCommitData = new FileData.FileCommitData(isFileSmelly, commitId, count, commitTime, loc, lcom, wmc, rfc, cbo, nom, noa, dit, noc);

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

    private static ArrayList<Double> getFileSlopesForMetricsBetween2Commits(FileData fd, FileCommitData beginFileDataIncluding, FileCommitData lastCommitDataExcluding) throws IOException
    {

        ArrayList<FileCommitData> slopeCommitsData = getAllCommitsDataInBetween(fd, beginFileDataIncluding, lastCommitDataExcluding);

        ArrayList<Double> slopes = null;

        if (slopeCommitsData.size() > 1) // to calculate the slope at least 2 commits needed
        {

            ArrayList<ArrayList<Double>> metricsForCommitsInBetween = new ArrayList<>();

            for (FileCommitData slopeCommitData : slopeCommitsData)
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
        FileData fd = parseFileWithItsData(filePath.toString());
        allFilesData.add(fd);

        //System.out.println(fd.fileNamePath);

        if (fd.willEverBecomeSmelly) //smelly file, read only one line when file became smelly
        {
            if (!SMELLY_CSV_FILE_NAMES.contains(fd.fileNamePath))
            {
                SMELLY_CSV_FILE_NAMES.add(fd.fileNamePath);
            }

            if (CLEAN_CSV_FILE_NAMES.contains(fd.fileNamePath)) //it was previously added as clean -> remove
            {
                CLEAN_CSV_FILE_NAMES.remove(fd.fileNamePath);
            }

            composeLineAndWriteToFile(fd, getCommitIdWhenFileBecomeSmelly(fd), PATH_TO_SMELLY_FINAL_RESULT_FILE);
        } else
        {
            CLEAN_CSV_FILE_NAMES.add(fd.fileNamePath);
        }

    }

    private static String makeUniqueString(String commitId, String futureCommitId)
    {
        String uniqueStr;
        uniqueStr = commitId.concat(futureCommitId);
        return uniqueStr;
    }


    private static void composeLineAndWriteToCleanFile(FileData fd, String lastEditCommitId, String metricsCommitId, int step, String outputFilePath) throws IOException, ParseException
    {
        String uniqueString = makeUniqueString(fd.fileNamePath, lastEditCommitId);

        if (!uniquePairs.contains(uniqueString))  //add all for smelly
        {
            String lineNumericalData = createFinalLineWithSlopesDataForCleanFile(fd, lastEditCommitId);
            lineNumericalData = lineNumericalData.replaceAll(",$", "");

            if (!lineNumericalData.isEmpty())
            {
                String finalLine = String.valueOf(new StringBuilder(fd.fileNamePath).append(",").
                        append(metricsCommitId).append(",").
                        append(step).append(",").
                        append(lineNumericalData));

                writeLineToFile(finalLine, outputFilePath);
            }
            uniquePairs.add(uniqueString);
        }

    }


    private static void composeLineAndWriteToFile(FileData fd, String commitIdWhereBecomesSmelly, String outputFilePath) throws IOException, ParseException
    {
        String uniqueString = makeUniqueString(fd.fileNamePath, commitIdWhereBecomesSmelly);

        if (!uniquePairs.contains(uniqueString) || fd.willEverBecomeSmelly)  //add all for smelly
        {
            String lineNumericalData = createFinalLineWithSlopesData(fd, commitIdWhereBecomesSmelly);

            if (!lineNumericalData.isEmpty())
            {
                String finalLine = String.valueOf(new StringBuilder(fd.fileNamePath).append(",").
                        append(commitIdWhereBecomesSmelly).append(",").
                        append(lineNumericalData));
                writeLineToFile(finalLine, outputFilePath);
            }
            uniquePairs.add(uniqueString);
        }

    }


    private static String getCommitIdFromAllHistoryBeforeNDays(String currentCommitId, int interval) throws ParseException
    {
        Calendar timeCurrentCommit = COMMIT_IDS_WITH_DATES.get(currentCommitId);
        Calendar dateBeforeAfterNDays = getDateBeforeOrAfterNDays(timeCurrentCommit, interval);
        ArrayList<String> commits = new ArrayList<>(COMMIT_IDS_WITH_DATES.keySet());

        if (interval < 0)
        {
            int i = commits.size() - 1;
            while (i > 0)
            {
                String commitId = commits.get(i);
                if (dateBeforeAfterNDays.after(COMMIT_IDS_WITH_DATES.get(commitId)))
                {
                    return commitId;
                }
                i--;
            }
        } else
        {
            int i = 0;
            while (i < commits.size())
            {
                String commitId = commits.get(i);
                if (dateBeforeAfterNDays.before(COMMIT_IDS_WITH_DATES.get(commitId)))
                {
                    return commitId;
                }
                i++;
            }
        }


        return "";
    }

    private static FileCommitData getCommitDataBeforeOrAfterInterval(FileData fd, String currentCommitId, int interval) throws ParseException
    {
        Calendar timeCurrentCommit = COMMIT_IDS_WITH_DATES.get(currentCommitId);
        //System.out.println(timeCurrentCommit);
        Calendar dateBeforeAfterNDays = getDateBeforeOrAfterNDays(timeCurrentCommit, interval);

        int currentCommitCount = COMMIT_IDS.indexOf(currentCommitId);
        int commitCountBeforeAfterNInterval = currentCommitCount + interval;


        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        int i = fileCommitDataArrayList.size();

        while (i > 0 && commitCountBeforeAfterNInterval > 2)
        {
            i--;
            FileCommitData fileCommitData = fileCommitDataArrayList.get(i);
            Calendar beforeAfterNewCommitDate = fileCommitData.time;
            int beforeAfterNewCommitCount = fileCommitData.commitCount;


            if (IS_PREDICTION_IN_DAYS)
            {
                if (interval < 0 && beforeAfterNewCommitDate.before(dateBeforeAfterNDays) && timeCurrentCommit.after(beforeAfterNewCommitDate) && beforeAfterNewCommitCount > 2)
                {
                    return fileCommitData;
                } else if (interval > 0 && beforeAfterNewCommitDate.after(dateBeforeAfterNDays) && timeCurrentCommit.before(beforeAfterNewCommitDate) && beforeAfterNewCommitCount > 0)
                {
                    return fileCommitData;
                }

            } else //in commits
            {
                if (beforeAfterNewCommitCount < commitCountBeforeAfterNInterval && timeCurrentCommit.after(beforeAfterNewCommitDate) && beforeAfterNewCommitCount > 2)
                {
                    return fileCommitData;
                }
            }

        }
        return null;
    }

    private static FileCommitData getRecentFileCommitData(FileData fd, FileCommitData metricsFileCommitData)
    {
        int recentCommitCount = 0;
        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;
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

    private static int getCountOfFileCommitData(FileData fd, FileCommitData fileCommitData)
    {
        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;
        return fileCommitDataArrayList.indexOf(fileCommitData);
    }


    private static ArrayList<Double> getMetricsOfFileCommitData(FileCommitData fileCommitData) throws IOException
    {
        @SuppressWarnings("UnnecessaryLocalVariable") ArrayList<Double> metrics = new ArrayList<>(Arrays.asList(fileCommitData.loc, fileCommitData.lcom, fileCommitData.wmc, fileCommitData.rfc, fileCommitData.cbo, fileCommitData.nom, fileCommitData.noa, fileCommitData.dit, fileCommitData.noc));

        return metrics;
    }


    private static FileCommitData getFileCommitDataFromCommitId(FileData fd, String commitId)
    {
        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        for (FileData.FileCommitData fileCommitData : fileCommitDataArrayList)
        {
            if (fileCommitData.commitId.equals(commitId))
            {
                return fileCommitData;
            }
        }
        return null;
    }

    private static String createFinalLineWithSlopesDataForCleanFile(FileData fd, String lastEditCommitId) throws IOException
    {

        FileCommitData lastEditFileCommitData = getFileCommitDataFromCommitId(fd, lastEditCommitId);

        FileCommitData recentFileCommitData = getRecentFileCommitData(fd, lastEditFileCommitData);

        ArrayList<Double> metrics = getMetricsOfFileCommitData(lastEditFileCommitData);

        FileCommitData firstFileCommitData = (FileCommitData) fd.fileCommitDataArrayList.get(0);

        ArrayList<Double> slopesHistory = getFileSlopesForMetricsBetween2Commits(fd, firstFileCommitData, lastEditFileCommitData);

        ArrayList<Double> slopesRecent;
        if (!firstFileCommitData.equals(recentFileCommitData))
        {
            slopesRecent = getFileSlopesForMetricsBetween2Commits(fd, recentFileCommitData, lastEditFileCommitData);
        } else
        {
            slopesRecent = slopesHistory;
        }


        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();

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

        return allMetricsAndSlopesForAllIntervals.toString();
    }


    private static String createFinalLineWithSlopesData(FileData fd, String futureCommitId) throws IOException, ParseException
    {
        int currentCount = STEP;

        String predictionCommitIdForInterval = "-";
        String systemMetricsCommitId = "-";

        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();
        StringBuilder resultLine = new StringBuilder();

        while (currentCount <= MAX_INT_DAYS_OR_COMMITS)
        {

            int randomNum = ThreadLocalRandom.current().nextInt(1, currentCount + 1);

            FileCommitData metricsFileCommitData = getCommitDataBeforeOrAfterInterval(fd, futureCommitId, -randomNum);

            boolean isSlopeChange = false;
            FileCommitData recentFileCommitData = null;
            ArrayList<Double> metrics = null;
            FileCommitData firstFileCommitData = null;
            ArrayList<Double> slopesHistory = null;

            if (metricsFileCommitData != null)
            {
                recentFileCommitData = getRecentFileCommitData(fd, metricsFileCommitData);

                metrics = getMetricsOfFileCommitData(metricsFileCommitData);

                firstFileCommitData = (FileCommitData) fd.fileCommitDataArrayList.get(0);

                slopesHistory = getFileSlopesForMetricsBetween2Commits(fd, firstFileCommitData, metricsFileCommitData);

                isSlopeChange = isAtLeastOneSlopeChange(slopesHistory);

                //metricsCommitIdForInterval = metricsFileCommitData.commitId;

                systemMetricsCommitId = getCommitIdFromAllHistoryBeforeNDays(futureCommitId, -randomNum);

                predictionCommitIdForInterval = getCommitIdFromAllHistoryBeforeNDays(systemMetricsCommitId, currentCount);

                //                FileCommitData predictionFileCommitData = getCommitDataBeforeOrAfterInterval(fd, metricsCommitIdForInterval, currentCount);
                //
                //                int survivalCount = currentCount;
                //                while(predictionFileCommitData == null && survivalCount > randomNum)
                //                {
                //                    survivalCount--;
                //                    predictionFileCommitData = getCommitDataBeforeOrAfterInterval(fd, metricsCommitIdForInterval, survivalCount);
                //                }
                //
                //                if (predictionFileCommitData != null)
                //                {
                //                    predictionCommitIdForInterval = predictionFileCommitData.commitId;
                //                }

            }


            if (metricsFileCommitData == null && currentCount == STEP) // metrics commit not found for the smallest interval
            {
                return "";
            } else if (metricsFileCommitData == null && currentCount > STEP) //there were slopes for at least one interval
            {
                int count = 0;
                while (count < 9 * 3 + 1 + 1 + 1)
                {
                    allMetricsAndSlopesForAllIntervals.append("-").append(",");
                    count++;
                }
            } else if (metricsFileCommitData != null && isSlopeChange)//slopes can be calculated for this interval
            {

                ArrayList<Double> slopesRecent;
                if (!firstFileCommitData.equals(recentFileCommitData))
                {
                    slopesRecent = getFileSlopesForMetricsBetween2Commits(fd, recentFileCommitData, metricsFileCommitData);
                } else
                {
                    slopesRecent = slopesHistory;
                }


                //substitute metrics commit Id, because last change commit was used


                allMetricsAndSlopesForAllIntervals.append(systemMetricsCommitId).append(",").append(randomNum).append(",");

                allMetricsAndSlopesForAllIntervals.append(predictionCommitIdForInterval).append(",");


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
                return "";
            }


            currentCount = currentCount + STEP; //15,30,45,60,75,90,105,120
        }

        resultLine.append(allMetricsAndSlopesForAllIntervals);


        if (fd.willEverBecomeSmelly)
        {
            ArrayList<String> currentSmells = getSmellsForFileInCommit(fd); //
            for (String smell : currentSmells) //5 smells
            {
                resultLine.append(smell).append(",");
            }
        }

        return resultLine.toString().replaceAll(",$", ""); //remove last comma

    }


    private static boolean isAtLeastOneSlopeChange(ArrayList<Double> slopes)
    {
        for (Double slope : slopes)
        {
            if (!(slope == 0))
            {
                return true;
            }
        }
        return false;
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


    private static ArrayList<String> getSmellsForFileInCommit(FileData fd) throws IOException
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


    private static ArrayList<FileCommitData> getAllCommitsDataInBetween(FileData fd, FileCommitData pastFileCommitData, FileCommitData currentFileCommitData)
    {
        ArrayList<FileCommitData> commitsDataInBetween = new ArrayList<>();
        int indexPast = getCountOfFileCommitData(fd, pastFileCommitData);
        int indexCurrent = getCountOfFileCommitData(fd, currentFileCommitData);

        for (int i = indexPast; i < indexCurrent; i++)
        {
            ArrayList<FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;
            commitsDataInBetween.add(fileCommitDataArrayList.get(i));
        }
        return commitsDataInBetween;
    }


}
