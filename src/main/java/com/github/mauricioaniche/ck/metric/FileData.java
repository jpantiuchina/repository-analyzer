package com.github.mauricioaniche.ck.metric;

public class FileData {

    private String file;
    private boolean isSmelly = false;
    private boolean isBlobClass = false;
    private boolean isClassDataSBP = false;
    private boolean isComplexClass = false;
    private boolean isFuncDecomp = false;
    private boolean isSpaghettiCode = false;

    private int cbo;
    private int wmc;
    private int dit;
    private int noc;
    private int rfc;
    private int lcom;
    private int nom;
    private int nopm;
    private int nosm;
    private int nof;
    private int nopf;
    private int nosf;
    private int nosi;
    private int loc;

    public FileData (String filePath){
        this.file = filePath;
    }


    public String getFile() { return this.file; }
    public void setFile (String file) {this.file = file; }

    public boolean getIsSmelly() { return this.isSmelly; }
    public void setIsSmelly (boolean isSmelly) {this.isSmelly = isSmelly; }

    public boolean getIsBlobClass() { return this.isBlobClass; }
    public void setIsBlobClass (boolean isBlobClass) {this.isBlobClass = isBlobClass; }

    public boolean getIsClassDataSBP() { return this.isClassDataSBP; }
    public void setIsClassDataSBP (boolean isClassDataSBP) {this.isClassDataSBP = isClassDataSBP; }

    public boolean getIsComplexClass() { return this.isComplexClass; }
    public void setIsComplexClass (boolean isComplexCalss) {this.isComplexClass = isComplexCalss; }

    public boolean getIisFuncDecomp() { return this.isFuncDecomp; }
    public void setIisFuncDecomp (boolean isFuncDecomp) {this.isFuncDecomp = isFuncDecomp; }

    public boolean getIsSpaghettiCode() { return this.isSpaghettiCode; }
    public void setIisSpaghettiCode (boolean isSpaghettiCode) {this.isSpaghettiCode = isSpaghettiCode; }


    public int getCBO()            { return this.cbo;  }
    public void setCBO(int cbo)    { this.cbo = cbo;   }

    public int getWMC()            { return this.wmc;  }
    public void setWMC(int wmc)    { this.wmc = wmc;   }

    public int getDIT()            { return this.dit;  }
    public void setDIT(int dit)    { this.dit = dit;   }

    public int getNOC()            { return this.noc;  }
    public void setNOC(int noc)    { this.noc = noc;   }

    public int getRFC()            { return this.rfc;  }
    public void setRFC(int rfc)    { this.rfc = rfc;   }

    public int getLCOM()           { return this.lcom; }
    public void setLCOM(int lcom)  { this.lcom = lcom; }

    public int getNOM()            { return this.nom;  }
    public void setNOM(int nom )   { this.nom = nom;   }

    public int getNOPM()           { return this.nopm; }
    public void setNOPM(int nopm)  { this.nopm = nopm; }

    public int getNOSM()           { return this.nosm; }
    public void setNOSM(int nosm)  { this.nosm = nosm; }

    public int getNOF()            { return this.nof;  }
    public void setNOF(int nof )   { this.nof = nof;   }

    public int getNOPF()           { return this.nopf; }
    public void setNOPF(int nopf)  { this.nopf = nopf;}

    public int getNOSF()           { return this.nosf; }
    public void setNOSF(int nosf)  { this.nosf = nosf; }

    public int getNOSI()           { return this.nosi; }
    public void setNOSI(int nosi)  { this. nosi= nosi; }

    public int getLOC()            { return this.loc;  }
    public void setLOC(int loc)    { this.loc = loc;   }

}
