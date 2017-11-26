package pipeline;

import java.util.ArrayList;
import java.util.Calendar;

class FileData
{

    String fileNamePath = "";
    boolean becomeSmelly = false;
    boolean isBlob = false;
    boolean isCDSBP = false;
    boolean isComplexClass = false;
    boolean isFuncDec = false;
    boolean isSpaghCode = false;
    ArrayList fileCommitDataArrayList = new ArrayList();
    FileData(String fileNamePath, boolean becomeSmelly, boolean isBlob, boolean isCDSBP, boolean isComplexClass, boolean isFuncDec, boolean isSpaghCode, ArrayList<FileCommitData> fileCommitDataArrayList)
    {
        this.fileNamePath = fileNamePath;
        this.becomeSmelly = becomeSmelly;
        this.isBlob = isBlob;
        this.isCDSBP = isCDSBP;
        this.isComplexClass = isComplexClass;
        this.isFuncDec = isFuncDec;
        this.isSpaghCode = isSpaghCode;
        this.fileCommitDataArrayList = fileCommitDataArrayList;
    }

    static boolean isFileSmelly(String fileData)
    {
        return fileData.contains("true");
    }

    static boolean isBlob(String smellType)
    {
        return smellType.contains("blob");
    }

    static boolean isCDSBP(String smellType)
    {
        return smellType.contains("cdsbp");
    }

    static boolean isComplexClass(String smellType)
    {
        return smellType.contains("complexClass");
    }

    static boolean isFuncDec(String smellType)
    {
        return smellType.contains("fd");
    }

    static boolean isSpaghCode(String smellType)
    {
        return smellType.contains("spaghettiCode");
    }

    static class FileCommitData
    {
        boolean isFileSmelly = false;

        double loc;
        double lcom;
        double wmc;// = 0;
        double rfc;// = 0;
        double cbo;// = 0;
        double nom;// = 0;
        double noa;// = 0;
        double dit;// = 0;
        double noc;// = 0;

        Calendar time;

        String commitId = "";

        int commitCount = 0;

        FileCommitData(boolean isFileSmelly, String commitId, int commitCount, Calendar time, double loc, double lcom, double wmc, double rfc, double cbo, double nom, double noa, double dit, double noc)
        {
            this.commitId = commitId;
            this.commitCount = commitCount;
            this.time = time;
            this.loc = loc;
            this.lcom = lcom;
            this.wmc = wmc;
            this.rfc = rfc;
            this.cbo = cbo;
            this.nom = nom;
            this.noa = noa;
            this.dit = dit;
            this.noc = noc;
            this.isFileSmelly = isFileSmelly;
        }
    }

}
