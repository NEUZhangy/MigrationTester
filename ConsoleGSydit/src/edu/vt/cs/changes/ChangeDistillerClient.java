package edu.vt.cs.changes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;

public class ChangeDistillerClient {
	
	public static final String VERSION = "default";

	public List<ChangeFact> parseChanges(String filePath) {
		File d = new File(filePath);
		File fd = new File(d.getAbsolutePath()+"/from");
		System.out.println("file numbers:" + fd.listFiles().length);
		Map<String, File> oldfiles = new HashMap<String, File>();
		String fileName = null;
		for(File f:fd.listFiles()){
			fileName = f.getName();
			if(fileName.endsWith(".java")&&fileName.indexOf("Test")<0){
				oldfiles.put(fileName, f);
			}
		}
		fd = new File(d.getAbsolutePath()+"/to");
		Map<String, File> newfiles = new HashMap<String, File>();
		for(File f:fd.listFiles()){
			fileName = f.getName();
			if(fileName.endsWith(".java")&&fileName.indexOf("Test")<0){
				newfiles.put(fileName, f);
			}
		}
		FileDistiller distiller = ChangeDistiller.createFileDistiller();
		FileDistiller.setIgnoreComments();
		Set<String> visited = new HashSet<String>();
		File oFile = null, nFile = null;
		ChangeFact cf = null;
		List<ChangeFact> cfList = new ArrayList<ChangeFact>();
		for (Entry<String, File> entry: oldfiles.entrySet()) {
			fileName = entry.getKey();
			visited.add(fileName);
			oFile = entry.getValue();
			nFile = newfiles.get(fileName);		
			if (!fileName.endsWith("java")) {
				continue;
			}
			try {
				if (oFile != null && nFile != null) {
			        String con_o = new String(Files.readAllBytes(Paths.get(oFile.getAbsolutePath())));
			        String con_n = new String(Files.readAllBytes(Paths.get(nFile.getAbsolutePath())));
			        String as_o = new String();
			        String as_n = new String();     
			        if (con_o.contains("extends PlayerListener") && con_n.contains("implements Listener")) {
			          as_o += "extends PlayerListener\n";
			          as_n += "implements Listener\n";
			        }
			        if (con_o.contains("@Override") && con_n.contains("@EventHandler")) {
			          as_o += "@Override";
			          as_n += "@EventHandler";
			        }
			        if (!as_o.isEmpty()) {
			          System.out.println("");
//			          DatabaseControl data1 = new DatabaseControl();
//			          int label = data1.insertpattern(as_o, as_n, CommonValue.common_old_version, CommonValue.common_new_version, "Interface Change", "1.5");   
//			          data1.insertsnippet(as_o, as_n, CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label));    
			        }
			        System.out.println();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			CommonValue.resetpure();
			distiller.extractClassifiedSourceCodeChanges(oFile, nFile);
//			CommonValue.pureanalysis();
			System.out.print("");
			cf = new ChangeFact(oFile, nFile, distiller.getSourceCodeChanges());
			cfList.add(cf);
		}
		oFile = null;
		for (Entry<String, File> entry : newfiles.entrySet()) {
			fileName = entry.getKey();
			if(!visited.add(fileName)) {
				continue;
			}// all newly added files			
			nFile = entry.getValue();
			distiller.extractClassifiedSourceCodeChanges(oFile, nFile);
			cf = new ChangeFact(oFile, nFile, distiller.getSourceCodeChanges());
			cfList.add(cf);
		}
		return cfList;
	}
}
