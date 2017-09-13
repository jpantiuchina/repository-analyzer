package com.github.mauricioaniche.ck;

import com.github.mauricioaniche.ck.metric.FileData;

import pipeline.SmellDetector;
import pipeline.WholePipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static pipeline.SmellDetector.readSmellsFromFileToHashmap;
//import java.util.ArrayList;

public class Runner {

	public static ArrayList<FileData> allFiles = new ArrayList<>();


	public static void main(String[] args) throws FileNotFoundException
	{
		if(args==null || args.length != 1) {
			System.out.println("Usage java -jar ck.jar <path to project>");
			System.exit(1);
		}

		String path = args[0];

		String csvPath = "output/computed_metrics.csv";

	}

	public static void computeQualityMetrics(String path, String csvPath, HashMap <String, ArrayList<String>> files) throws FileNotFoundException
	{

		CKReport report = new CK().calculate(path);

		PrintStream ps = new PrintStream(csvPath);
		ps.println("file,isSmelly,IsBlob,IsClassDataSBPCBO,IsComplexClass,IsFuncDecomp,IsSpaghettiCode,CBO, WMC,DIT,NOC,RFC,LCOM,NOM,NOPM,NOSM,NOF,NOPF,NOSF,NOSI,LOC");

		for(CKNumber result : report.all()) {
			if(result.isError()) continue;

			Path pathAbsolute = Paths.get(result.getFile());
			Path pathBase = Paths.get(path).toAbsolutePath();
			Path pathRelative = pathBase.relativize(pathAbsolute);
			//System.err.println(pathRelative);
			String newPath = pathRelative.toString().replaceFirst("\\.java$", "").replace(File.separatorChar, '.');
			//System.err.println(newPath);

			FileData classFileData = new FileData(newPath);

			ArrayList<String> smells = files.get(newPath);


			//System.err.println("SMELLS: " + smells +" for file " + newPath);

			if (smells != null)
			{
				for (String smell : smells)
				{
					if (smell.contains("god"))
					{
						classFileData.setIsBlobClass(true);
					}
					if (smell.contains("cdsbp"))
					{
						classFileData.setIsClassDataSBP(true);
					}
					if (smell.contains("complex"))
					{
						classFileData.setIsComplexClass(true);
					}
					if (smell.contains("spagh"))
					{
						classFileData.setIisSpaghettiCode(true);
					}
					if (smell.contains("func"))
					{
						classFileData.setIisFuncDecomp(true);
					}
					classFileData.setIsSmelly(true);
					//System.err.println("Setting smell " + smell + " to file " + classFileData.getFile());
				}
				classFileData.setIsSmelly(true);
			}

			classFileData.setCBO (result.getCbo());
			classFileData.setWMC (result.getWmc());
			classFileData.setDIT (result.getDit());
			classFileData.setNOC (result.getNoc());
			classFileData.setRFC (result.getRfc());
			classFileData.setLCOM(result.getLcom());
			classFileData.setNOM (result.getNom());
			classFileData.setNOPM(result.getNopm());
			classFileData.setNOSM(result.getNosm());
			classFileData.setNOF (result.getNof());
			classFileData.setNOPF(result.getNopf());
			classFileData.setNOSF(result.getNosf());
			classFileData.setNOSI(result.getNosi());
			classFileData.setLOC (result.getLoc());




			allFiles.add(classFileData);

			ps.println(
					classFileData.getFile() + "," +
							classFileData.getIsSmelly() + "," +
							classFileData.getIsBlobClass() + "," +
							classFileData.getIsClassDataSBP() + "," +
							classFileData.getIsComplexClass() + "," +
							classFileData.getIisFuncDecomp() + "," +
							classFileData.getIsSpaghettiCode() + "," +
							result.getCbo() + "," +
							result.getWmc() + "," +
							result.getDit() + "," +
							result.getNoc() + "," +
							result.getRfc() + "," +
							result.getLcom() + "," +
							result.getNom() + "," +
							result.getNopm() + "," +
							result.getNosm() + "," +
							result.getNof() + "," +
							result.getNopf() + "," +
							result.getNosf() + "," +
							result.getNosi() + "," +
							result.getLoc()
			);
		}
		ps.close();
	}
}
