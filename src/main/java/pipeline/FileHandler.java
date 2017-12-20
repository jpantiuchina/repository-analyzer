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
import static java.lang.Integer.parseInt;
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

    static void handleAllFilesForRepo(File[] allFiles) throws IOException, ParseException, InterruptedException
    {
        if (allFiles != null)
        {
            for (File file : allFiles)
            {
                if (file.isFile())
                {
                    FileData fd = parseFileWithItsData(file.toString());
                    allFilesData.add(fd);

                    // add file names
                    if (!CSV_FILE_NAMES.contains(fd.fileNamePath))
                    {
                        CSV_FILE_NAMES.add(fd.fileNamePath);
                    }
                   // System.out.println("Reading data from file " + file);
                }
            }
        }
    }

    static void handleSmellyFiles() throws ParseException, InterruptedException, IOException
    {
        for (FileData fd : allFilesData)
        {
            handleSmellyFileAndAddFinalFileDataLines(fd);

        }

    }


//    private static ArrayList<String> getOnlyCleanFileNames(ArrayList<String> allFileNames, String metricsCommitId, String predictionCommitId)
//    {
//        ArrayList<String> cleanFileNames = new ArrayList<>();
//        for (String fileName : allFileNames)
//        {
//            if (!CSV_FILE_NAMES.contains(fileName) && !cleanFileNames.contains(fileName))
//            {
//                cleanFileNames.add(fileName);
//            }
//        }
//        return cleanFileNames;
//    }


    private static HashMap<FileData, String> getCommitIdOfPreviousEditAndReturnFileData(String fileName, String currentCommitId)
    {
        HashMap<FileData, String> fileDataAndCommitId = new HashMap<>();
        int count = COMMIT_ID_TO_COMMIT_INDEX.get(currentCommitId);

        for (FileData fd : allFilesData)
        {
            if (fd.fileNamePath.equals(fileName))
            {
                ArrayList<FileCommitData> fileCommitDataList = fd.fileCommitDataArrayList;

                for (int i = fileCommitDataList.size() - 1; i >= 0; i--)
                {
                    String commitId = fileCommitDataList.get(i).commitId;
                    int commitCount = COMMIT_ID_TO_COMMIT_INDEX.get(commitId);
                    if (commitCount <= count) //commit which was before
                    {
                        fileDataAndCommitId.put(fd, commitId);
                        return fileDataAndCommitId;
                    }
                }
                return null; //not found, first commit
            }
        }

        return null;
    }


    private static int getTotalNumOfSteps(int totalCols)
    {
        return (totalCols - 8) / 29;
    }


//        static int getCommitCountWhenFileWasRemoved(String fileName, String commitId) throws IOException, InterruptedException
//    {
//        int removedInCommitCount = 0;
//        int currentCommitCount = COMMIT_IDS.indexOf(commitId);
//        while (currentCommitCount < COMMIT_IDS.size() - 1)
//        {
//            currentCommitCount++;
//            if (!getAllFileNamesPresentInCommitId(COMMIT_IDS.get(currentCommitCount)).contains(fileName))
//            {
//                return currentCommitCount;
//            }
//        }
//        return 0;
//    }


    static void addCleanFilesForEverySmellyFileInCommit() throws IOException, InterruptedException, ParseException
    {
        try (BufferedReader br = new BufferedReader(new FileReader(PATH_TO_SMELLY_FINAL_RESULT_FILE)))
        {
            @SuppressWarnings("UnusedAssignment") String line = br.readLine(); //skip headerLine

            while ((line = br.readLine()) != null) //read lines from smelly files
            {
                //System.out.println(line);
                String[] cols = line.split(","); // use comma as separator

            //    System.out.println("Adding clean files for smelly metrics commits for file: " + cols[0]);

                int totalCols = cols.length;

                int totalNumberOfSteps = getTotalNumOfSteps(totalCols);
                int currentStep = 1;
                while (currentStep <= totalNumberOfSteps)
                {
                    //int metricsCommitIdColumnForStep = 3 + 31(x-1)
                    int metricsCommitIdColumnForStep = 31 * currentStep - 28;
                    String metricsCommitId = cols[metricsCommitIdColumnForStep];
                    String metricsCommitCount = cols[metricsCommitIdColumnForStep + 1];
                    String predictionCommitId = cols[metricsCommitIdColumnForStep + 2];
                    String predictionCommitCount = cols[metricsCommitIdColumnForStep + 3];

                    //to get the system's snapshots, we need all smelly files in final cleanFolder file except smelly files
                    if (!metricsCommitId.equals("-") && !predictionCommitId.equals("-") && !predictionCommitId.equals("null"))
                    {
                        ArrayList<String> allFileNamesInMetricsCommit = getAllFileNamesPresentInCommitId(metricsCommitId); //git show will return all smelly and clean files
                        //check if file is not present in the list of smelly file names
                        //ArrayList<String> cleanFileNames = getOnlyCleanFileNames(allFileNamesInMetricsCommit, metricsCommitId, predictionCommitId);

                        ArrayList<String> allFileNamesInPredictionCommit = getAllFileNamesPresentInCommitId(predictionCommitId); //git show will return all smelly and clean files

                        if (!allFileNamesInMetricsCommit.isEmpty())
                        {
                            for (String fileNameInMetricsCommit : allFileNamesInMetricsCommit)
                            {
                                if (allFileNamesInPredictionCommit.contains(fileNameInMetricsCommit))
                                {
                                    HashMap<FileData, String> fileDataAndCommitIdOfPreviousEditInMetrics = getCommitIdOfPreviousEditAndReturnFileData(fileNameInMetricsCommit, metricsCommitId);
                                    HashMap<FileData, String> fileDataAndCommitIdOfPreviousEditInPrediction = getCommitIdOfPreviousEditAndReturnFileData(fileNameInMetricsCommit, predictionCommitId);

                                    if (fileDataAndCommitIdOfPreviousEditInMetrics != null && fileDataAndCommitIdOfPreviousEditInPrediction != null)
                                    {
                                        FileData fd = fileDataAndCommitIdOfPreviousEditInMetrics.keySet().iterator().next();
                                        String previousEditOfMetricsCommitId = fileDataAndCommitIdOfPreviousEditInMetrics.get(fd);

                                        FileData pfd = fileDataAndCommitIdOfPreviousEditInMetrics.keySet().iterator().next();
                                        String previousEditOfPredictionCommitId = fileDataAndCommitIdOfPreviousEditInMetrics.get(pfd);

                                        ArrayList<FileCommitData> metricsFileCommits = fd.fileCommitDataArrayList;
                                        FileCommitData predictionCommitData = findCommitDataInArrayList(metricsFileCommits,previousEditOfPredictionCommitId);
                                        int predictionCount = predictionCommitData.commitCount;

                                        for (int i = 0; i < metricsFileCommits.size(); i++)
                                        {
                                            FileCommitData metricsFileCommit = metricsFileCommits.get(i);
                                            if (metricsFileCommit.commitId.equals(previousEditOfMetricsCommitId) && metricsFileCommit.commitCount > 2)
                                            {
                                                boolean clean = true;
                                                //check if this file is clean until previousEditOfPredictionCommitId
                                                while (i < predictionCount - 1)
                                                {
                                                    i++;
                                                    FileCommitData inBetweenCommit = metricsFileCommits.get(i);
                                                    if(inBetweenCommit.isFileSmelly)
                                                    {
                                                        clean = false;
                                                    }
                                                }
                                                if (clean)
                                                {
                                                    composeLineAndWriteToCleanFile(fd, metricsFileCommit, previousEditOfMetricsCommitId, metricsCommitId, metricsCommitCount, predictionCommitId, predictionCommitCount, PATH_TO_CLEAN_FINAL_RESULT_FILE);
                                                }
                                                break;
                                            }
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


    private static FileCommitData findCommitDataInArrayList(ArrayList<FileCommitData> metricsFileCommits, String commitId)
    {
        for (FileCommitData predictionCommit: metricsFileCommits)
        {
            if (predictionCommit.commitId.equals(commitId))
            {
                return predictionCommit;
            }
        }
        return null;
    }

    private static String getFileNameAsInFileButNotGitShow(String fileNameInGitShow)
    {
        for (String fileNme : CSV_FILE_NAMES)
        {
            if (fileNme.contains(fileNameInGitShow) || fileNameInGitShow.contains(fileNme))
            {
                return fileNme;
            }
        }

        return "";
    }


    private static String makeFileNameFromAbsolutePath(String fileName)
    {
        return fileName.replaceAll("^.*.java.", "");
    }

    static ArrayList<String> getAllFileNamesPresentInCommitId(String commitId) throws IOException, InterruptedException
    {

        ArrayList<String> output = executeCommandsAndReadLinesFromConsole(new File(REPO_FOLDER_NAME), "git", "ls-tree", "--name-only", "-r", commitId);
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


    private static void handleSmellyFileAndAddFinalFileDataLines(FileData fd) throws IOException, ParseException, InterruptedException
    {
        if (fd.willEverBecomeSmelly) //smelly file, read only one line when file became smelly
        {
            composeLineAndWriteToSmellyFile(fd, getCommitIdWhenFileBecomeSmelly(fd), PATH_TO_SMELLY_FINAL_RESULT_FILE);
        }
    }

    private static String makeUniqueString(String commitId, String futureCommitId, String fileName)
    {
        String uniqueStr;
        uniqueStr = commitId.concat(futureCommitId).concat(fileName);
        return uniqueStr;
    }


    private static void composeLineAndWriteToCleanFile(FileData fd, FileCommitData lastEditFileCommitData, String lastEditCommitId, String metricsCommitId, String metricsCommitCount, String predictionCommitId, String predictionCommitCount, String outputFilePath) throws IOException, ParseException, InterruptedException
    {
        String uniqueString = makeUniqueString(fd.fileNamePath, metricsCommitId, predictionCommitId);

        if (!uniquePairs.contains(uniqueString))  //add all for smelly
        {
            String lineNumericalData = createFinalLineWithSlopesDataForCleanFile(fd, lastEditFileCommitData, metricsCommitId, predictionCommitId);
            lineNumericalData = lineNumericalData.replaceAll(",$", "");

            if (!lineNumericalData.isEmpty())
            {
                String finalLine = String.valueOf(new StringBuilder(fd.fileNamePath).append(",").
                        append(metricsCommitId).append(",").
                        append(metricsCommitCount).append(",").
                        append(predictionCommitId).append(",").
                        append(predictionCommitCount).append(",").
                        //append(getCommitCountWhenFileWasRemoved(fd.fileNamePath, metricsCommitId)).append(",").
                        append(lineNumericalData));

                writeLineToFile(finalLine, outputFilePath);
            }
            uniquePairs.add(uniqueString);
        }

    }


    private static void composeLineAndWriteToSmellyFile(FileData fd, String commitIdWhereBecomesSmelly, String outputFilePath) throws IOException, ParseException, InterruptedException
    {
        //String uniqueString = makeUniqueString(fd.fileNamePath, commitIdWhereBecomesSmelly);

        //additional check
        if (fd.willEverBecomeSmelly)
        {
            String lineNumericalData = createFinalLineWithSlopesData(fd, commitIdWhereBecomesSmelly);

            if (!lineNumericalData.isEmpty())
            {
//                System.out.println();
//                System.out.println("Adding smells data for file: " + fd.fileNamePath);
//                System.out.println();
                String finalLine = String.valueOf(new StringBuilder(fd.fileNamePath).append(",").
                        append(COMMIT_ID_TO_COMMIT_INDEX.get(commitIdWhereBecomesSmelly)).append(",").
                        append(commitIdWhereBecomesSmelly).append(",").
                        //append(getCommitCountWhenFileWasRemoved(fd.fileNamePath, commitIdWhereBecomesSmelly)).append(",").
                        append(lineNumericalData));
                writeLineToFile(finalLine, outputFilePath);
            }
            //uniquePairs.add(uniqueString);
        }

    }


    private static String getCommitIdFromAllHistoryBeforeNDays(String currentCommitId, int interval) throws ParseException
    {
        Calendar timeCurrentCommit = COMMIT_IDS_WITH_DATES.get(currentCommitId);
        Calendar dateBeforeAfterNDays = getDateBeforeOrAfterNDays(timeCurrentCommit, interval);
        ArrayList<String> commits = new ArrayList<>(COMMIT_IDS_WITH_DATES.keySet());

        if (interval == 0)
        {
            return currentCommitId;
        }

        else if (interval < 0)
        {
            for (int commitsCount = commits.size() - 1; commitsCount > 2; commitsCount--)
            {
                String beforeOfAfterCommitId = commits.get(commitsCount);
                Calendar beforeAfterNewCommitTime = COMMIT_IDS_WITH_DATES.get(beforeOfAfterCommitId);
                if (beforeAfterNewCommitTime.before(dateBeforeAfterNDays) && COMMIT_ID_TO_COMMIT_INDEX.get(beforeOfAfterCommitId) < COMMIT_ID_TO_COMMIT_INDEX.get(currentCommitId))
                {
                    return beforeOfAfterCommitId;
                }
            }
        } else
        {
            for (int commitsCount = 0; commitsCount < commits.size(); commitsCount++)
            {
                String beforeOfAfterCommitId = commits.get(commitsCount);
                Calendar beforeAfterNewCommitTime = COMMIT_IDS_WITH_DATES.get(beforeOfAfterCommitId);
                if (beforeAfterNewCommitTime.after(dateBeforeAfterNDays) && COMMIT_ID_TO_COMMIT_INDEX.get(beforeOfAfterCommitId) > COMMIT_ID_TO_COMMIT_INDEX.get(currentCommitId))
                {
                    return beforeOfAfterCommitId;
                }
            }
        }
        return "";
    }

    private static FileCommitData getCommitDataBeforeOrAfterInterval(FileData fd, String currentCommitId, int interval) throws ParseException
    {
        ArrayList<FileData.FileCommitData> fileCommitDataArrayList = fd.fileCommitDataArrayList;

        if (fileCommitDataArrayList.size() < 3)
        {
            return null;
        }

        Calendar timeCurrentCommit = COMMIT_IDS_WITH_DATES.get(currentCommitId);
        Calendar dateBeforeAfterNDays = getDateBeforeOrAfterNDays(timeCurrentCommit, interval);


        if (interval < 0) //go backwards
        {

            for (int commitsCount = fileCommitDataArrayList.size() - 1; commitsCount > 2; commitsCount--)
            {
                FileCommitData fileCommitData = fileCommitDataArrayList.get(commitsCount);
                Calendar beforeAfterNewCommitTime = fileCommitData.time;
                if (beforeAfterNewCommitTime.before(dateBeforeAfterNDays))
                {
                    return fileCommitData;
                }
            }
        }

        if (interval > 0)
        {
            for (int commitsCount = 2; commitsCount < fileCommitDataArrayList.size(); commitsCount++)
            {
                FileCommitData fileCommitData = fileCommitDataArrayList.get(commitsCount);
                Calendar beforeAfterNewCommitTime = fileCommitData.time;
                if (beforeAfterNewCommitTime.after(dateBeforeAfterNDays))
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
        //int metricsCommitCount = getCountOfFileCommitData(fd, metricsFileCommitData);
        int metricsCommitCount = metricsFileCommitData.commitCount;
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

    private static String createFinalLineWithSlopesDataForCleanFile(FileData fd, FileCommitData lastEditFileCommitData, String metricsCommitId, String predictionCommitId) throws IOException
    {

        //FileCommitData lastEditFileCommitData = getFileCommitDataFromCommitId(fd, lastEditCommitId);

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


    private static String createFinalLineWithSlopesData(FileData fd, String commitIdWhereBecomesSmelly) throws IOException, ParseException, InterruptedException
    {
        int currentCount = STEP;

        String predictionCommitIdForInterval = "-";
        String systemMetricsCommitId = "-";

        StringBuilder allMetricsAndSlopesForAllIntervals = new StringBuilder();
        StringBuilder resultLine = new StringBuilder();

        while (currentCount <= MAX_INT_DAYS_OR_COMMITS)
        {

            int randomNum = ThreadLocalRandom.current().nextInt(1, currentCount + 1);

            FileCommitData metricsFileCommitData = getCommitDataBeforeOrAfterInterval(fd, commitIdWhereBecomesSmelly, -randomNum);

            boolean isSlopeChange = false;
            FileCommitData recentFileCommitData = null;
            ArrayList<Double> metrics = null;
            FileCommitData firstFileCommitData = null;
            ArrayList<Double> slopesHistory = null;

            if (metricsFileCommitData != null && !metricsFileCommitData.isFileSmelly)
            {
                recentFileCommitData = getRecentFileCommitData(fd, metricsFileCommitData);

                metrics = getMetricsOfFileCommitData(metricsFileCommitData);

                firstFileCommitData = (FileCommitData) fd.fileCommitDataArrayList.get(0);

                slopesHistory = getFileSlopesForMetricsBetween2Commits(fd, firstFileCommitData, metricsFileCommitData);

                isSlopeChange = isAtLeastOneSlopeChange(slopesHistory);


                systemMetricsCommitId = getCommitIdFromAllHistoryBeforeNDays(commitIdWhereBecomesSmelly, -randomNum);
                predictionCommitIdForInterval = getCommitIdFromAllHistoryBeforeNDays(commitIdWhereBecomesSmelly, currentCount-randomNum);
                //ArrayList<String> filesInPredictionCommit = getAllFileNamesPresentInCommitId(predictionCommitIdForInterval);

//                boolean found = false;
//                while (!filesInPredictionCommit.contains(fd.fileNamePath) && randomNum < currentCount && !found) //file is not present in prediction commit
//                {
//                        randomNum++;
//                        predictionCommitIdForInterval = getCommitIdFromAllHistoryBeforeNDays(commitIdWhereBecomesSmelly, currentCount-randomNum);
//                        filesInPredictionCommit = getAllFileNamesPresentInCommitId(predictionCommitIdForInterval);
//                        if (filesInPredictionCommit.contains(fd.fileNamePath))
//                        {
//                            found = true;
//                        }
//                }
//
//                if (!found && !filesInPredictionCommit.contains(fd.fileNamePath))
//                {
//                    predictionCommitIdForInterval = commitIdWhereBecomesSmelly;
//                }

            }


            if (metricsFileCommitData == null && currentCount == STEP) // metrics commit not found for the smallest interval
            {
                return "";
            }

            else if (metricsFileCommitData == null && currentCount > STEP) //there were slopes for at least one interval
            {
                int count = 0;
                while (count < 9 * 3 + 4)
                {
                    allMetricsAndSlopesForAllIntervals.append("-").append(",");
                    count++;
                }
            }

            else if (metricsFileCommitData != null && isSlopeChange)//slopes can be calculated for this interval
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

//                System.out.println(COMMIT_ID_TO_COMMIT_INDEX.get(systemMetricsCommitId));
//                System.out.println(COMMIT_ID_TO_COMMIT_INDEX.get(predictionCommitIdForInterval));
//                System.out.println();

                allMetricsAndSlopesForAllIntervals.
                        append(systemMetricsCommitId).append(",").
                        append(COMMIT_ID_TO_COMMIT_INDEX.get(systemMetricsCommitId)).append(",").
                        append(predictionCommitIdForInterval).append(",").
                        append(COMMIT_ID_TO_COMMIT_INDEX.get(predictionCommitIdForInterval)).append(","); //predictionCommitCount

                //allMetricsAndSlopesForAllIntervals.append(predictionCommitIdForInterval).append(",");


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
