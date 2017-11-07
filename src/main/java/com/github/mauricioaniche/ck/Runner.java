package com.github.mauricioaniche.ck;

import com.github.mauricioaniche.ck.metric.FileData;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

//import static pipeline.WholePipeline.PATH_TO_REPOSITORY;

public class Runner {

//	public static void computeQualityMetricsAndSmellsForCommitAndSaveToFile(String pathToQualityMetricsResultFile, HashMap <String, ArrayList<String>> fileNamesWithSmells) throws FileNotFoundException
//	{
//
//		CKReport report = new CK().calculate(String.valueOf(PATH_TO_REPOSITORY));
//
//		PrintStream ps = new PrintStream(pathToQualityMetricsResultFile);
//
//		try
//		{
//		ps.println("file,isSmelly,IsBlob,isCoupling,IsNPath,CBO,WMC,DIT,NOC,RFC,LCOM,NOM,NOPM,NOSM,NOF,NOPF,NOSF,NOSI,LOC");
//
//		for(CKNumber result : report.all())
//		{
//			if(result.isError()) continue;
//
//			Path pathAbsolute = Paths.get(result.getFile());
//
//			String newPath = pathAbsolute.toString();
//
//			FileData classFileData = new FileData(newPath);
//
//
//			ArrayList<String> smells = fileNamesWithSmells.get(newPath);
//
//			if (smells != null)
//			{
//
//				for (String smell : smells)
//				{
//					if (smell.contains("God"))
//					{
//						classFileData.setIsBlobClass(true);
//					}
//					if (smell.contains("Coupling"))
//					{
//						classFileData.setIsCoupling(true);
//					}
//					if (smell.contains("NPath"))
//					{
//						classFileData.setNPath(true);
//					}
//					classFileData.setIsSmelly();
//					//System.err.println("Setting smell " + smell + " to file " + classFileData.getFile());
//				}
//				classFileData.setIsSmelly();
//			}
//
//			classFileData.setCBO (result.getCbo());
//			classFileData.setWMC (result.getWmc());
//			classFileData.setDIT (result.getDit());
//			classFileData.setNOC (result.getNoc());
//			classFileData.setRFC (result.getRfc());
//			classFileData.setLCOM(result.getLcom());
//			classFileData.setNOM (result.getNom());
//			classFileData.setNOPM(result.getNopm());
//			classFileData.setNOSM(result.getNosm());
//			classFileData.setNOF (result.getNof());
//			classFileData.setNOPF(result.getNopf());
//			classFileData.setNOSF(result.getNosf());
//			classFileData.setNOSI(result.getNosi());
//			classFileData.setLOC (result.getLoc());
//
//			ps.println(
//					classFileData.getFile() + "," +
//							classFileData.getIsSmelly() + "," +
//							classFileData.getIsBlobClass() + "," +
//							classFileData.getIsCoupling() + "," +
//							classFileData.getNPath() + "," +
//							result.getCbo() + "," +
//							result.getWmc() + "," +
//							result.getDit() + "," +
//							result.getNoc() + "," +
//							result.getRfc() + "," +
//							result.getLcom() + "," +
//							result.getNom() + "," +
//							result.getNopm() + "," +
//							result.getNosm() + "," +
//							result.getNof() + "," +
//							result.getNopf() + "," +
//							result.getNosf() + "," +
//							result.getNosi() + "," +
//							result.getLoc()
//			);
//		}
//		}
//		finally
//		{
//			ps.close();
//
//		}
//	}
}
