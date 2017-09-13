package pipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;


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

        runSmellDetector(path, PATH_TO_RESULT_FILE);

        readSmellsFromFileToHashmap(PATH_TO_RESULT_FILE);
    }

    static void runSmellDetector(String path, String PATH_TO_RESULT_FILE) throws IOException
    {
        String operatingSystem = System.getProperty("os.name");

        //call cs-detector
        if (operatingSystem.contains("Mac"))
        {
            //for Mac
            Git.executeCommandsAndReadLinesFromConsole("/bin/bash", "-c", "java -jar cs-detector.jar " + path + " " + PATH_TO_RESULT_FILE);
            System.out.println(" Smells were written to file: " + PATH_TO_RESULT_FILE);
        }
        else if (operatingSystem.contains("Windows"))
        {
            //for Windows
            Git.executeCommandsAndReadLinesFromConsole("cmd /c java -jar cs-detector.jar " + path + " " + PATH_TO_RESULT_FILE);

        }
        else
        {
            ResultFileWriter.log();
            System.err.println("The program works only on Mac and Windows OS's");
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

            String[] line = sCurrentLine.split(",");
            fileName = line[0];
            smellType = line[1];

            ArrayList<String> smells = files.get(fileName);
            //if filename is not present in the hashmap
            if (smells == null)
            {
                smells = new ArrayList<>();
                files.put(fileName,smells);
            }

            System.err.println(" FILENAME WITH SMELL: "+fileName);
            System.err.println(" SMELL TYPE: "+smellType);
            System.err.println(" ALREDY PRESENT SMELLS "+smells);

            smells.add(smellType);
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
    return files;
}





}
