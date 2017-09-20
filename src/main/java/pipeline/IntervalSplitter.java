package pipeline;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import static pipeline.ResultFileWriter.log;
import static pipeline.ResultFileWriter.removeFileIfPresent;
import static pipeline.ResultFileWriter.writeLineToFile;
import static pipeline.SlopeCalculation.getSlopeForFileMetric;


class IntervalSplitter
{
    static ArrayList<String> commits = new ArrayList<>();


    static ArrayList<String> getAllCommitIdsFromConsoleLines(List<String> linesFromConsole) throws IOException, ParseException {

        ArrayList<String> commitIds = new ArrayList<>();
        for (String line : linesFromConsole)
        {
            String commit = line.substring(0, 40);
            commitIds.add(commit);
        }

        if (commitIds.isEmpty()) {
            log();
            System.err.print("No commits");
            System.exit(5);
        }

        return commitIds;
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


    static void handleGroups(List<List<String>> groups) throws IOException
    {

        for (int i = 1; i <= groups.size() - 2; i++) // // from second group to last but one
        {
            String pastCommit    = groups.get(i - 1).get(0);
            String currentCommit = groups.get(i    ).get(0);
            String futureCommit  = groups.get(i + 1).get(0);

            System.out.println("Past commit:    " + pastCommit         );
            System.out.println("Current commit: " + currentCommit      );
            System.out.println("Future commit:  " + futureCommit + "\n");

            handleThreeCommits(pastCommit,currentCommit,futureCommit);
        }
    }

    static void handleThreeCommits(String pastCommitId, String currentCommitId, String futureCommitId) throws IOException
    {
        ArrayList<String> pastFiles = getAllFileNamesInCommit(pastCommitId);
        ArrayList<String> currentFiles = getAllFileNamesInCommit(currentCommitId);
        ArrayList<String> futureFiles = getAllFileNamesInCommit(futureCommitId);

        ArrayList<String> commonFiles = getIntersectionOfFilesInCommits(pastFiles,currentFiles,futureFiles);

        for (String fileName : commonFiles)
        {
            handleFile(pastCommitId, currentCommitId, futureCommitId, fileName);

        }



    }

    static void handleFile(String pastCommitId, String currentCommitId, String futureCommitId, String fileName) throws IOException
    {
        ArrayList<String> commitsInBetween = getAllCommitsBetween(pastCommitId,currentCommitId);

        //retrieve all quality metrics for the specific file for each commit in the interval
        ArrayList<ArrayList<Double>> metricsForFile = getQualityMetricsForFileInCommits(fileName,commitsInBetween);
        ArrayList<Double> slopes = getSlopeForAllFileMetrics(metricsForFile);

        //create output file with all data : filename, commitId, current metrics, slopes, current smells, future smells
        AddFileDataToFinalResultFile(slopes,currentCommitId,futureCommitId,fileName);

    }


    static void AddFileDataToFinalResultFile(ArrayList<Double> slopes,String currentCommitId, String futureCommitId, String fileName) throws IOException
    {
        StringBuilder resultLine = new StringBuilder();

        resultLine.append(fileName).append(",").append(currentCommitId).append(",");

        ArrayList<Double> metrics = getQualityMetricsForFileInCommit(fileName,currentCommitId); //14 quality metrics
        for (Double m : metrics)
        {
            resultLine.append(m);
            resultLine.append(",");
        }

        for (Double sl : slopes) //14 slopes
        {
            resultLine.append(sl);
            resultLine.append(",");
        }


        ArrayList<String> smells = getAllSmellsForFileFromCommit(fileName,currentCommitId); //1+5 smells
        for (String s : smells)
        {
            resultLine.append(s);
            resultLine.append(",");
        }

        ArrayList<String> smellsFuture = getAllSmellsForFileFromCommit(fileName,futureCommitId); //1+5 smells
        for (String sf : smellsFuture)
        {
            resultLine.append(sf);
            resultLine.append(",");
        }
        //remove last comma and add resultLine to output file
        writeLineToFile(resultLine.toString().replaceAll(",$", ""),"output/final.csv");
    }


    static ArrayList<String> getAllSmellsForFileFromCommit(String fileName, String commitId) throws IOException
    {
        ArrayList<String> smells = new ArrayList<>();

        File inputFile = new File("output/"+commitId+".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(",");
            // use comma as separator
            if (cols[0].equals(fileName)) //found line with searched filename
            {
                // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                int i = 1;
                //System.out.println(cols.length);
                while ( i < 7) //smells in csv file are from 1 to 7 column
                {
                    //System.out.println(cols[i]);
                    smells.add((cols[i]));
                    i++;
                }
            }
        }
        return smells;

    }



    static void createEmptyFinalResultFile() throws IOException
    {   //removes previous result file
        PrintStream ps = new PrintStream("output/final.csv");
        ps.println("file,commitId," +
                "CBO,      WMC,      DIT,      NOC,      RFC,      LCOM,      NOM,      NOPM,      NOSM,      NOF,      NOPF,      NOSF,      NOSI,      LOC," +
                        "CBOslope, WMCslope, DITslope, NOCslope, RFCslope, LCOMslope, NOMslope, NOPMslope, NOSMslope, NOFslope, NOPFslope, NOSFslope, NOSIslope, LOCslope, " +
                        "isSmelly,         IsBlob,         IsClassDataSBP,         IsComplexClass,          IsFuncDecomp,         IsSpaghettiCode,"+
                        "isSmellyInFuture, IsBlobInFuture, IsClassDataSBPInFuture, IsComplexClassFInFuture, IsFuncDecompInFuture, IsSpaghettiCodeInFuture"
                );
        ps.close();
//        writeLineToFile("file,commitId," +
//                        "CBO,      WMC,      DIT,      NOC,      RFC,      LCOM,      NOM,      NOPM,      NOSM,      NOF,      NOPF,      NOSF,      NOSI,      LOC," +
//                        "CBOslope, WMCslope, DITslope, NOCslope, RFCslope, LCOMslope, NOMslope, NOPMslope, NOSMslope, NOFslope, NOPFslope, NOSFslope, NOSIslope, LOCslope, " +
//                        "isSmelly,         IsBlob,         IsClassDataSBP,         IsComplexClass,          IsFuncDecomp,         IsSpaghettiCode,"+
//                        "isSmellyInFuture, IsBlobInFuture, IsClassDataSBPInFuture, IsComplexClassFInFuture, IsFuncDecompInFuture, IsSpaghettiCodeInFuture",
//                        "output/final.csv");

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

        for (int i = 0; i < commits.size(); i++)
        {
            String commitId = commits.get(i);
            metricsForFile.add(getQualityMetricsForFileInCommit(fileName,commitId)); //add all metrics for fileName to hashmap
        }

        return metricsForFile;
    }

    static ArrayList<Double> getQualityMetricsForFileInCommit(String fileName, String commitId) throws IOException
    {
        ArrayList<Double> metrics = new ArrayList<>();

        File inputFile = new File("output/"+commitId+".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(","); // use comma as separator
            if (cols[0].equals(fileName)) //found line with searched filename
            {
               // System.out.println(cols[0] + " FILENAME: " + fileName + " ARE EQUAL? " + cols[0].equals(fileName));

                int i = 7; //quality metrics in csv file start from column 7
                //System.out.println(cols.length);
                while ( i < cols.length)
                {
                    //System.out.println(cols[i]);
                    metrics.add(Double.parseDouble(cols[i]));
                    i++;
                }
            }
        }
        return metrics;

    }


    static ArrayList getAllCommitsBetween(String pastCommitId, String currentCommitId)
    {
        ArrayList<String> commitsInBetween = new ArrayList<>();
        int indexPast = commits.indexOf(pastCommitId);
        int indexCurrent = commits.indexOf(currentCommitId);
        //System.out.println("IP= "+indexPast);
        //System.out.println("IC="+indexCurrent);
        for (int i = 0; i < commits.size(); i++)
        {
            //System.out.println("i= " +i);
            if (i >= indexPast  && i <= indexCurrent)
            {
                commitsInBetween.add(commits.get(i));
            }
        }
        return commitsInBetween;
    }



    static ArrayList getAllFileNamesInCommit(String commitId) throws IOException
    {
        ArrayList fileNames = new ArrayList();
        File inputFile = new File("output/"+commitId+".csv");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(",");
            // use comma as separator
            fileNames.add(cols[0]);
            //System.err.println("FILENAME: " + cols[0]);
        }
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
