package pipeline;

public class FileCommit
{
    public JavaFile javaFile;
    public Commit Commit;

    boolean isFileSmelly = false;

    double loc;
    double lcom;
    double wmc;
    double rfc;
    double cbo;
    double nom;
    double noa;
    double dit;
    double noc;

    boolean isBlob;
    boolean isCDSBP;
    boolean isComplexClass;
    boolean isSpaghCode;
    boolean isFuncDec;

    public FileCommit(double loc, double lcom, double wmc, double rfc, double cbo, double nom, double noa, double dit, double noc,
            boolean isFileSmelly, boolean isBlob, boolean isCDSBP, boolean isComplexClass, boolean isSpaghCode, boolean isFuncDec
            )
    {
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
        this.isBlob = isBlob;
        this.isCDSBP = isCDSBP;
        this.isComplexClass = isComplexClass;
        this.isSpaghCode = isSpaghCode;
        this.isFuncDec = isFuncDec;
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



}


}
