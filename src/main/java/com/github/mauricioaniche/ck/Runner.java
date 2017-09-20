package com.github.mauricioaniche.ck;

import com.github.mauricioaniche.ck.metric.FileData;

import pipeline.SmellDetector;
import pipeline.WholePipeline;

import javax.sound.midi.Soundbank;
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

	public static void computeQualityMetrics(String pathToRepository, String pathToQualityMetricsResultFile, HashMap <String, ArrayList<String>> fileNamesWithSmells) throws FileNotFoundException
	{

		CKReport report = new CK().calculate(pathToRepository);

	//	System.out.println("Path: " + pathToRepository);

		PrintStream ps = new PrintStream(pathToQualityMetricsResultFile);
		ps.println("file,isSmelly,IsBlob,isCoupling,IsNPath,CBO,WMC,DIT,NOC,RFC,LCOM,NOM,NOPM,NOSM,NOF,NOPF,NOSF,NOSI,LOC");

		for(CKNumber result : report.all()) {
			if(result.isError()) continue;

			Path pathAbsolute = Paths.get(result.getFile());
			Path pathBase = Paths.get(pathToRepository).toAbsolutePath();
			//Path pathRelative = pathBase.relativize(pathAbsolute);
			//System.err.println(pathRelative);
			String newPath = pathAbsolute.toString();

			FileData classFileData = new FileData(newPath);


			ArrayList<String> smells = fileNamesWithSmells.get(newPath);

			if (smells != null)
			{
//				System.out.println("===");
//				System.out.println("fileNameWithSmells: " + fileNamesWithSmells);
//				System.out.println("newPath: " + newPath);
//				System.out.println("smells: " + smells);
//				System.out.println("===");
//				System.out.println("SMELLS: " + smells +" for file " + newPath);
//

				for (String smell : smells)
				{
					if (smell.contains("God"))
					{
						classFileData.setIsBlobClass(true);
					}
					if (smell.contains("Coupling"))
					{
						classFileData.setIsCoupling(true);
					}
					if (smell.contains("NPath"))
					{
						classFileData.setNPath(true);
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
							classFileData.getIsCoupling() + "," +
							classFileData.getNPath() + "," +
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
