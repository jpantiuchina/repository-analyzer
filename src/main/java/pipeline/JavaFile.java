package pipeline;

import java.util.ArrayList;

class JavaFile
{

    String fileNamePath = "";
    boolean becomeSmelly = false;

    ArrayList fileCommitArrayList = new ArrayList();

    JavaFile(String fileNamePath, boolean becomeSmelly, ArrayList<Commit> fileCommitArrayList)
    {
        this.fileNamePath = fileNamePath;
        this.becomeSmelly = becomeSmelly;
        this.fileCommitArrayList = fileCommitArrayList;
    }

}
