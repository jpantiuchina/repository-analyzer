package pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static pipeline.Util.log;
import static pipeline.WholePipeline.FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD;
import static pipeline.WholePipeline.PATH_TO_REPOSITORY;


class SmellDetector
{

    static void runSmellDetector(String pathToSmellsResultFile) throws IOException, InterruptedException
    {
        log();

        ArrayList<String> linesFromConsoleRunningPmd = Git.executeCommandsAndReadLinesFromConsole(new File("."),
                "./run.sh", //script runs pmd smell detector
                PATH_TO_REPOSITORY.toString(),
                pathToSmellsResultFile);

        for (String line : linesFromConsoleRunningPmd)
        {
            Util.writeLineToFile(line, FILE_PATH_TO_LINES_FROM_CONSOLE_RUNNING_PMD);
        }
    }


    //read smells from the file and save to hashmap
static HashMap <String,ArrayList<String>> readSmellsFromFileToHashmap(String filePath)
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

            ArrayList<String> smells = files.computeIfAbsent(fileName, k -> new ArrayList<>());
            //if filename is not present in the hashmap

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
