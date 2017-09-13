package pipeline;

import com.github.mauricioaniche.ck.Runner;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static pipeline.IntervalSplitter.*;
import static pipeline.ResultFileWriter.log;
import static pipeline.SmellDetector.readSmellsFromFileToHashmap;

/**
 *  Input: 2 arguments:
 *  1) the address of a git repository hosted on GitHub
 *  2) time interval expressed in days
 *
 *  https://github.com/jpantiuchina/robot 5
 *  https://github.com/square/okhttp 500
 */
public class WholePipeline
{
    //public static HashMap<String,String> smellTypes = new HashMap();

    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        if(args==null || args.length != 2)
        {
            log();
            System.out.println("Usage arguments: <repository url> <time interval in days>");
            System.exit(1);
        }

        String repository = args[0];
        int interval = Integer.parseInt(args[1]);

        System.out.println("Repository: " + repository + " ; Interval of days: " + interval);

        Git.clone_repository(repository);
        Git.retrieveCommits(); //runs git log in reverse order & saves whole history in output/all_commits.txt

        IntervalSplitter.groups = IntervalSplitter.getCommitsAtIntervalsFromArrayList(interval);

        IntervalSplitter.printCommitsInIntervals(); // prints to console & saves grouped commits to result.txt

        createOutputFileforEachCommit();
        createEmptyFinalResultFile();

        handleGroups(groups);

    }


    public static void createOutputFileforEachCommit() throws IOException
    {
        for (int i = 0; i < IntervalSplitter.groups.size(); i++)
        {
            List<String> group = IntervalSplitter.groups.get(i);

            for (String commit : group)
            {
                System.out.print(commit);
                //checkout each commit of the whole repository
                Git.executeCommandsAndReadLinesFromConsole("/bin/bash", "-c", "cd " + Git.PATH_TO_REPOSITORY + " && git checkout " + commit);
                String commitFileName = "output/".concat(commit).concat(".csv");
                //run smell detector
                String commitSmells = "output/".concat(commit).concat("-smells.csv");
                SmellDetector.runSmellDetector("output/cloned_repository",commitSmells);
                //add smells to hashmap
                HashMap <String, ArrayList<String>> files = readSmellsFromFileToHashmap(commitSmells);

                Runner.computeQualityMetrics("output/cloned_repository", commitFileName, files);
            }
            System.out.println();
        }



    }


}
