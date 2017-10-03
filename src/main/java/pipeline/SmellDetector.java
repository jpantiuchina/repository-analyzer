package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

//import static pipeline.Git.PATH_TO_REPOSITORY;
//import static pipeline.ResultFileWriter.OUTPUT_FOLDER_NAME;
import static pipeline.WholePipeline.PATH_TO_REPOSITORY;
import static pipeline.WholePipeline.LINES_FROM_CONSOLE_RUNNING_PMD;


/**
 * runs cs-detector.jar
 */
public class SmellDetector
{

    static void runSmellDetector(String pathToSmellsResultFile) throws IOException, InterruptedException
    {
        //log();

        Git.executeCommandsAndReadLinesFromConsole(new File("."),
                "./run.sh",
                PATH_TO_REPOSITORY.toString(),
                pathToSmellsResultFile);

//        Git.executeCommandsAndReadLinesFromConsole(new File("."), "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY + " -f csv -R java-coupling |grep 'CouplingBetweenObjects' >>" + pathToSmellsResultFile );
//        Git.executeCommandsAndReadLinesFromConsole(new File("."), "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY
//                + " -f csv -R java-codesize |grep 'NPathComplexity' >>" + pathToSmellsResultFile );
//

//


        //old way
//        Git.executeCommandsAndReadLinesFromConsoleOLD(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY + " -f csv -R java-design |grep 'God' >" + pathToSmellsResultFile );
//        Git.executeCommandsAndReadLinesFromConsoleOLD(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY + " -f csv -R java-coupling |grep 'CouplingBetweenObjects' >>" + pathToSmellsResultFile );
//        Git.executeCommandsAndReadLinesFromConsoleOLD(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY
//                + " -f csv -R java-codesize |grep 'NPathComplexity' >>" + pathToSmellsResultFile );
//

    }


    //read smells from the file and save to hashmap
public static HashMap <String,ArrayList<String>> readSmellsFromFileToHashmap(String filePath)
{
    HashMap <String, ArrayList<String>> files = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath)))
    {

        String sCurrentLine;
        String fileName;
        String smellType;

        while ((sCurrentLine = br.readLine()) != null) {

            String[] line = sCurrentLine.split("(?<=\"),(?=\")");
            fileName = line[2].replace("\"","");
            smellType = line[7].replace("\"","");

            ArrayList<String> smells = files.get(fileName);
            //if filename is not present in the hashmap
            if (smells == null)
            {
                smells = new ArrayList<>();
                files.put(fileName, smells);
            }

            //add smellType only if it is not present
            if (!smells.contains(smellType))
            smells.add(smellType);
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
    return files;
}





}
