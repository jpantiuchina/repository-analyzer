package pipeline;

import java.util.Calendar;

class Commit
{
    boolean isFileSmelly = false;

    double loc;
    double lcom;
    double wmc;
    double rfc;
    double cbo;// = 0;
    double nom;// = 0;
    double noa;// = 0;
    double dit;// = 0;
    double noc;// = 0;

    Calendar time;

    String commitId = "";

    int commitCount = 0;

    Commit(boolean isFileSmelly, String commitId, int commitCount, Calendar time, double loc, double lcom, double wmc, double rfc, double cbo, double nom, double noa, double dit, double noc)
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
