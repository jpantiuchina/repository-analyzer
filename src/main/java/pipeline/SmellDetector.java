package pipeline;

import java.io.BufferedReader;
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
    //static HashMap<String, ArrayList<String>> fileSmells = new HashMap<>();


    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
//        Path pathAbsolute = Paths.get("/Users/jennya/IdeaProjects/my_thesis/output/cloned_repository/okhttp/src/main/java/com/squareup/okhttp/internal/spdy/SpdyConnection.java");
//        Path pathBase = Paths.get("output/cloned_repository").toAbsolutePath();
//        Path pathRelative = pathBase.relativize(pathAbsolute);
//        System.out.println(pathRelative);
//        System.out.println(pathRelative.toString().replaceFirst("\\.java$", "").replace(File.separatorChar, '.'));
//        System.exit(55);

        if(args==null || args.length != 1)
        {
            ResultFileWriter.log();
            System.out.println("Usage argument: <path to the project>");
            System.exit(1);
        }

        String path = args[0];
        String PATH_TO_RESULT_FILE = "output/smells.csv";

        System.out.println("Project path: " + path + " ; csv with smells: " + PATH_TO_RESULT_FILE);

        Path pathAbsolute = Paths.get(path);
        Path pathBase = Paths.get("output/cloned_repository").toAbsolutePath();
        Path pathRelative = pathBase.relativize(pathAbsolute);
        //System.out.println(pathRelative);
        //System.out.println(pathRelative.toString().replaceFirst("\\.java$", "").replace(File.separatorChar, '.'));

        //runSmellDetector(path, PATH_TO_RESULT_FILE);

        readSmellsFromFileToHashmap(PATH_TO_RESULT_FILE);
    }

    static void runSmellDetector(String PATH_TO_RESULT_FILE) throws IOException
    {
        String operatingSystem = System.getProperty("os.name");

        //call pmd for 3 types of smells
        if (operatingSystem.contains("Mac") || operatingSystem.contains("Linux"))
        {
            //for Mac
            Git.executeCommandsAndReadLinesFromConsole(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY + " -f csv -R java-design |grep 'God' >" + PATH_TO_RESULT_FILE );
            Git.executeCommandsAndReadLinesFromConsole(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY + " -f csv -R java-coupling |grep 'CouplingBetweenObjects' >>" + PATH_TO_RESULT_FILE );
            Git.executeCommandsAndReadLinesFromConsole(LINES_FROM_CONSOLE_RUNNING_PMD, "/bin/bash", "-c", "pmd-bin-5.8.1/bin/run.sh pmd -d " + PATH_TO_REPOSITORY
                    + " -f csv -R java-codesize |grep 'NPathComplexity' >>" + PATH_TO_RESULT_FILE );

           // System.out.println(" Smells were written to file: " + PATH_TO_RESULT_FILE);
        }
//        else if (operatingSystem.contains("Windows"))
//        {
//            //for Windows
//            Git.executeCommandsAndReadLinesFromConsole(LINES_FROM_CONSOLE_RUNNING_PMD,"cmd /c java -jar cs-detector.jar " + pathToRepository + " " + PATH_TO_RESULT_FILE);
//
//        }
        else
        {
            ResultFileWriter.log();
            System.err.println("The program works only on Mac or Linux");
            System.exit(1);
        }

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
//            System.err.println("filePath: " + filePath);
//            System.err.println(Arrays.toString(line));
//            System.err.println(sCurrentLine);
            fileName = line[2].replace("\"","");
            smellType = line[7].replace("\"","");

            ArrayList<String> smells = files.get(fileName);
            //if filename is not present in the hashmap
            if (smells == null)
            {
                smells = new ArrayList<>();
                files.put(fileName, smells);
            }

//            System.err.println("---");
//            System.err.println(" FILENAME WITH SMELL: " + fileName);
//            System.err.println(" SMELL TYPE: " + smellType);
//            System.err.println();
//            System.err.println(" ALREDY PRESENT SMELLS " + smells);
//            System.err.println("---");

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
