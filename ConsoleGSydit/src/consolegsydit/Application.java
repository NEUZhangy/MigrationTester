package consolegsydit;

//import infra2.if2Common;
//import infrasupport.CommonValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

import applytool.applyController;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import partial.code.grapa.commit.CommitComparator;
import partial.code.grapa.version.detect.VersionDetector;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.changes.ChangeDistillerClient;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.CommitComparatorClient;

public class Application implements IApplication{

	boolean processDerby = false;
	boolean applying = false;
	static String old_version = new String();
	
	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		ChangeDistillerClient client = new ChangeDistillerClient();
		String folderName = null;
		if (processDerby) {
//			System.out.println("wrong way");
		} 
		else if (applying) {
//			List<String> patternList = readtxt(CommonValue.applyworkspace+CommonValue.patternList);
//			String[] patternSitu = patternList.split(",");
//			one_test(patternSitu[0], patternSitu[1], patternSitu[2], patternSitu[3],
//					"lucene" ,"large_lucene", "org.apache.lucene");
			List<String> applyList = readtxt(CommonValue.applyworkspace+CommonValue.applyList);
			for (String oneapply: applyList) {
				String[] applySitu = oneapply.split(",");
				applyController A = new applyController("/home/shengzhe/Desktop/PLDI2019/light_weight_apply_tool_test/static_patternfile.txt");	
				String inputcode = CommonValue.dataspace+"client_code/"+ "large_lucene" +"/" + applySitu[0] +"/"+applySitu[1]+"/";
				String oldc = A.readToString(inputcode+"from/"+applySitu[4]);
				A.check_code(oldc);
				A.writetofile(inputcode+"to/"+applySitu[4]);
				//check dep
				one_test(applySitu[0], applySitu[1], applySitu[2], applySitu[3],
						"lucene" ,"large_lucene", "org.apache.lucene");
				String pt_dep = A.getDep();
				System.out.println("patterndep:" + pt_dep);
				boolean label = false;
				if (pt_dep == null) label = true;
				System.out.println("appdep:" + CommonValue.all_dep.toString());
				for (String depRcd: CommonValue.all_dep) {
					if (depRcd.equals(pt_dep)) {
						label = true;
					}
				}
				//end
				if (label == true) {
					System.out.println(A.getNewCode());
					System.out.println("success!");
				}
			}

		}
		else{
//			System.out.println("this way Shengzhe");
			long startTime = System.nanoTime();			
//			String old_version = "2.3.2";
//			String new_version = "4.7.0";
//			String project_name = "nuxeo_data";
//			String commit_brach = "9cbf49";
//			String lib_name = "lucene";
//			CommonValue.set_possible_name("org.apache.lucene");
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "nuxeo_data";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("nuxeo", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "2.3.2", "4.7.0");
			
			// Only 1 entity migration --> Work
//			String old_version = "4.2.0";
//			String new_version = "4.3.0";
//			String project_name = "fave100-lucene";
//			String commit_brach = "eeae62";
//			String lib_name = "lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.possible_lib_name2 = "L" + CommonValue.join("/", CommonValue.possible_lib_name1.split("[.]"));
//			folderName = "Done_data_fave100-lucene";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "4.0.0";
//			String new_version = "3.6.0";
//			String project_name = "fave100-lucene";
//			String commit_brach = "eeae62";
//			String lib_name = "lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.possible_lib_name2 = "L" + CommonValue.join("/", CommonValue.possible_lib_name1.split("[.]"));
//			folderName = "1easycase";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			// entity migration --> Work
//			String old_version = "4.1.0";
//			String new_version = "4.2.0";
//			String project_name = "fave100-tomcat";
//			String lib_name = "lucene";
//			String commit_brach = "35e16a";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.possible_lib_name2 = "L" + CommonValue.join("/", CommonValue.possible_lib_name1.split("[.]"));
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "Done_data_fave100-tomcat/35e16a";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			//Types --> Work but not in lucene
//			String old_version = "1.4.3";
//			String new_version = "2.9.0";
//			String project_name = "roller";
//			String lib_name = "lucene";
//			String commit_brach = "779be8";
//			Common.common_old_version = lib_name + old_version;
//			Common.common_new_version = lib_name + new_version;
//			Common.common_project_name = project_name;
//			Common.common_commit_number = commit_brach;
//			folderName = "Done_data_roller";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);


//			String old_version = "4.10.2";
//			String new_version = "5.0.0";
//			String project_name = "elasticsearch-lang-python";
//			String lib_name = "lucene";
//			String commit_brach = "7634b5";
//			Common.common_old_version = lib_name + old_version;
//			Common.common_new_version = lib_name + new_version;
//			Common.common_project_name = project_name;
//			Common.common_commit_number = commit_brach;
//			folderName = "Done_data_elasticsearch-lang-python";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			// Both type and method migration
//			String old_version = "3.6.1";
//			String new_version = "5.0.0";
//			String project_name = "ae-interface";
//			String lib_name = "lucene";
//			String commit_brach = "1551b5";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_ae-interface";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("ae-interface", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "3.6.1", "5.0.0");
		
			

			//Method Migration
//			String old_version = "3.5.0";
//			String new_version = "4.1.0";
//			String project_name = "elasticsearch-analysis-edgengram2";
//			String lib_name = "lucene";
//			String commit_brach = "dd91b3";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_elasticsearch-analysis-edgengram2";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			System.out.println("Parse OK");
//			CommitComparatorClient client2 = new CommitComparatorClient("elasticsearch-analysis-edgengram2", "lucene");
//			System.out.println("Comparator OK");
//			client2.analyzeCommitForMigration(cfList, folderName, "3.5.0", "4.1.0");
			
//			String old_version = "2.2.0";
//			String new_version = "2.3.0";
//			String commit_brach = "10e0a6";
//			String project_name = "hibernate-search";
//			String lib_name = "lucene";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_hibernate-search";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("hibernate-search", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "2.2.0", "2.3.0");
						
//			String old_version = "3.5.0";
//			String new_version = "4.1.0";
//			String commit_brach = "7634b5";
//			String project_name = "elasticsearch-analysis-edgengram2";
//			String lib_name = "lucene";
//			Common.common_old_version = lib_name + old_version;
//			Common.common_new_version = lib_name + new_version;
//			Common.common_project_name = project_name;
//			Common.common_commit_number = commit_brach;
//			folderName = "data_elasticsearch-analysis-edgengram2";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("elasticsearch-analysis-edgengram2", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "3.5.0", "4.1.0");
//			
//			String old_version = "2.4.1";
//			String new_version = "3.0.3";
//			String commit_brach = "10e0a6";
//			String project_name = "jackrabbit";
//			String lib_name = "lucene";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_jackrabbit";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("jackrabbit", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "2.4.1", "3.0.3");
			
//			String old_version = "3.0.2";
//			String new_version = "3.1.0";
//			String project_name = "mahout-twitter";
//			String lib_name = "lucene";
//			String commit_brach = "26d99f";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_mahout-twitter";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("mahout-twitter", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "3.0.2", "3.1.0");
			
//			String old_version = "3.1.0";
//			String new_version = "4.6.0";
//			String project_name = "Ibn-Taymiyyah";
//			String lib_name = "lucene";
//			String commit_brach = "26d99f";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_Ibn-Taymiyyah";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient("Ibn-Taymiyyah", "lucene");
//			client2.analyzeCommitForMigration(cfList, folderName, "3.1.0", "4.6.0");
			
//			String old_version = "2.4.1";
//			String new_version = "2.9.1";
//			String project_name = "hibernate-search";
//			String lib_name = "lucene";
//			String commit_brach = "26d99f";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "data_hibernate-search";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			//level3
//			String old_version = "2.3.2";
//			String new_version = "2.4.1";
//			String commit_brach = "9ae431";
////			String old_version = "2.4.1";
////			String new_version = "3.0.3";
////			String commit_brach = "10e0a6";
//			String project_name = "jackrabbit";
//			String lib_name = "lucene";
//			Common.common_old_version = lib_name + old_version;
//			Common.common_new_version = lib_name + new_version;
//			Common.common_project_name = project_name;
//			Common.common_commit_number = commit_brach;
//			folderName = "data_jackrabbit";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "1.5.1";
//			String new_version = "1.5.2";
//			String project_name = "1AddOre";
//			String lib_name = "bukkit";
//			String commit_brach = "bc30d1bd3f74485b0b891988f0b9885f93b9276d";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "1AddOre/bc30d1bd3f74485b0b891988f0b9885f93b9276d";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/bkt_test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "1.0.0";
//			String new_version = "1.1";
//			String project_name = "2BuyCommand";
//			String lib_name = "bukkit";
//			String commit_brach = "fd0f0a5dfb6329aabc2c6468fe2d645f35b1831b";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "2BuyCommand/fd0f0a5dfb6329aabc2c6468fe2d645f35b1831b";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/bkt_test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "3.5.0";
//			String new_version = "4.0.0";
//			String project_name = "8lucene-multilingual";
//			String lib_name = "lucene";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			String commit_brach = "fc6522cdc31a29e64fe958f340e5e0e497b8f17b";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "fc6522cdc31a29e64fe958f340e5e0e497b8f17b";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "2.4.1";
//			String new_version = "3.0.0";
//			String project_name = "sthsth";
//			String lib_name = "lucene";
//			String commit_brach = "b64598";
//			CommonValue.possible_lib_name1 = "org.apache.lucene";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "b64598";
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/1case2pattern/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "25";
//			String new_version = "23";
//			String project_name = "5yoctolib_android";
//			String lib_name = "android";
//			CommonValue.possible_lib_name1 = "android";
//			String commit_brach = "f73f800087a198fa7764210bc148835a45ee5b9d";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
////			folderName = "8MiWoTreff/8b76e602b6f22237bec64008b0358976b9b5af93";
//			folderName = "5yoctolib_android/f73f800087a198fa7764210bc148835a45ee5b9d";			
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/android1all/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "23";
//			String new_version = "25";
//			String project_name = "5yoctolib_android";
//			String lib_name = "android";
//			CommonValue.set_possible_name("android");
//			String commit_brach = "f73f800087a198fa7764210bc148835a45ee5b9d";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
////			folderName = "8MiWoTreff/8b76e602b6f22237bec64008b0358976b9b5af93";
//			folderName = "5yoctolib_android/f73f800087a198fa7764210bc148835a45ee5b9d";			
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/android1all/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "23";
//			String new_version = "24";
//			String project_name = "5yoctolib_android";
//			String lib_name = "android";
//			CommonValue.set_possible_name("android");
//			String commit_brach = "f3590c02366c3929d676c4ebd189f086f8ef2ba9";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
////			folderName = "8MiWoTreff/8b76e602b6f22237bec64008b0358976b9b5af93";
//			folderName = "4quickMemo/f3590c02366c3929d676c4ebd189f086f8ef2ba9";			
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/largescaletest/large_android/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			//to be reopened 
//			
//			String old_version = "4.1.0";
//			String new_version = "4.2.0";
//			String project_name = "5highlight-elasticsearch";
//			String lib_name = "lucene";
//			CommonValue.set_possible_name("org.apache.lucene");
//			String commit_brach = "11bf7a8b1a7ec88e4d38ee69c8b5c577001fb68d";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "5highlight-elasticsearch/11bf7a8b1a7ec88e4d38ee69c8b5c577001fb68d";			
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/largescaletest/large_lucene/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			//one of most complecated situation
//			String old_version = "24";
//			String new_version = "25";
//			String project_name = "cf_test";
//			String lib_name = "android";
//			CommonValue.possible_lib_name1 = "android";
//			String commit_brach = "1234";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
////			folderName = "8MiWoTreff/8b76e602b6f22237bec64008b0358976b9b5af93";
//			folderName = "1234";			
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/cf_test/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
//			String old_version = "1.0.0";
//			String new_version = "1.2.5";
//			String project_name = "1BlockLoot";
//			String lib_name = "bukkit";
//			CommonValue.possible_lib_name1 = "org.bukkit";
//			String commit_brach = "4c9bf02cd96fd6e0cfce2f6d18ee66283153ef3b";
//			CommonValue.common_old_version = lib_name + old_version;
//			CommonValue.common_new_version = lib_name + new_version;
//			CommonValue.common_project_name = project_name;
//			CommonValue.common_commit_number = commit_brach;
//			folderName = "1BlockLoot/4c9bf02cd96fd6e0cfce2f6d18ee66283153ef3b";	
//			List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/craftbukkitall/" + folderName);
//			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
//			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
			
			
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/lucene_lib.txt",
//					"/home/shengzhex/Documents/research_repo/luna_api_migration/lucene_list.csv",
//					"lucene" ,"largescaletest/large_lucene", "org.apache.lucene");
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/bkt_lib.txt",
//					"/home/shengzhex/Documents/research_repo/luna_api_migration/bukkit_list.csv");
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/adr_lib.txt",
//					"/home/shengzhex/Documents/research_repo/luna_api_migration/android_list_1.csv");
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/adr_lib2_result.txt",
//					"/home/shengzhex/Documents/research_repo/luna_api_migration/android_list_2.csv");
			
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/lucene_lib.txt",
//			"/home/shengzhex/Documents/research_repo/luna_api_migration/largescaletest/large_lucene/large_lucene.csv",
//				"lucene" ,"large_lucene", "org.apache.lucene");
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/adr_lib.txt",
//			"/home/shengzhex/Documents/research_repo/luna_api_migration/largescaletest/large_android/large_android.csv",
//				"android", "large_android", "android");
			
			
//			readFileByLines("/home/shengzhex/Documents/research_repo/luna_api_migration/bkt_lib.txt",
//					"/home/shengzhex/Documents/research_repo/luna_api_migration/largescaletest/large_bukkit/large_bukkit.csv",
//				"bukkit","large_bukkit", "org.bukkit");
			
//			readFileByLines(CommonValue.dataspace+"client_code/large_lucene/lucene_lib.txt",
//					CommonValue.dataspace+"client_code/large_lucene/large_lucene.csv",
//						"lucene" ,"large_lucene", "org.apache.lucene");
			
//			readFileByLines(CommonValue.dataspace+"client_code/large_bukkit/bkt_lib.txt",
//					CommonValue.dataspace+"client_code/large_bukkit/large_bukkit.csv",
//						"bukkit","large_bukkit", "org.bukkit");
			
			readFileByLines(CommonValue.dataspace+"client_code/large_android/adr_lib.txt",
					CommonValue.dataspace+"client_code/large_android/large_android.csv",
				"android", "large_android", "android");
			
//			readFileByLines(CommonValue.dataspace+"client_code/large_commonio/commonio_lib.txt",
//					CommonValue.dataspace+"client_code/large_commonio/commonio_clientcode_list.csv",
//						"commonsio" ,"large_commonio", "org.apache.commons");
			
//			readFileByLines("/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_springsec/springsec_lib.txt",
//					"/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_springsec/springsec_clientcode_list.csv",
//						"springsec" ,"large_springsec", "org.springframework");
			
//			readFileByLines("/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_mockito/Mockito19_lib.txt",
//					"/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_mockito/mockito19_clientcode_list.csv",
//						"mockito" ,"large_mockito", "org.mockito");
			
//			readFileByLines("/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_guava/guava_lib.txt",
//					"/home/shengzhex/Desktop/research_repo/api_migration_data_folder/client_code/large_guava/guava_list.csv",
//						"guava" ,"large_guava", "com.google");
			

			long endTime = System.nanoTime();
			long duration = (endTime - startTime)/1000000;
			System.out.println(duration + "ms which means " + duration/1000 +"s");
		}		
		return null;
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub	
	}
	
	public List<String> readtxt(String filename) {
		List<String> sb= new ArrayList<String>();
		try {
            // prepare pattern from file
            
            FileReader reader = new FileReader(filename);
            BufferedReader br = new BufferedReader(reader);
            String str = null;
           
            while((str = br.readLine()) != null) {
                sb.add(str);  
//            	sb += str+",\n";
//                  System.out.println(str);
            }
           
            br.close();
            reader.close();
        }
        catch(IOException e) {
        	e.printStackTrace();
        }
		return sb;
	}
	
	
	public void one_test(String project_name, String commit_brach, String old_version, String new_version,
			String lib_name, String data_name, String lib_check) {
//		String old_version = "2.4.1";
//		String new_version = "2.9.1";
//		String project_name = "hibernate-search";
//		String lib_name = "android";
//		String data_name = "android2all";
//		String lib_name = "lucene";
//		String data_name = "allinall";
//		String lib_name = "bukkit";
//		String data_name = "craftbukkitall";
//		String lib_name = "android";
//		String data_name = "largescaletest/large_android";
//		String lib_name = "lucene";
//		String data_name = "largescaletest/large_lucene";
//		String lib_name = "bukkit";
//		String data_name = "largescaletest/large_bukkit";
		
//		String commit_brach = "26d99f";
		CommonValue.common_old_version = lib_name + old_version;
		CommonValue.common_new_version = lib_name + new_version;
		CommonValue.common_project_name = project_name;
		CommonValue.common_commit_number = commit_brach;
		CommonValue.all_dep = new ArrayList<String>();
//		CommonValue.possible_lib_name1 = "org.apache.lucene";
//		CommonValue.possible_lib_name1 = "org.bukkit";
//		CommonValue.possible_lib_name1 = "android";
//		CommonValue.possible_lib_name1 = lib_check;
		CommonValue.set_possible_name(lib_check);
		ChangeDistillerClient client = new ChangeDistillerClient();
		String folderName = project_name + "/" + commit_brach;
		System.out.println(folderName);
		System.out.println(old_version + ":" + new_version);
//		List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/"+ data_name +"/" + folderName);
		try {
			List<ChangeFact> cfList = client.parseChanges(CommonValue.dataspace+"client_code/"+ data_name +"/" + folderName);
			CommitComparatorClient client2 = new CommitComparatorClient(project_name, lib_name);
			client2.analyzeCommitForMigration(cfList, folderName, old_version, new_version);
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public void readFileByLines(String libName, String fileName, String lib_name, String data_name, String lib_check) {  
        File file = new File(libName);  
        BufferedReader reader = null;  
        HashMap<String, String> oldvMap = new HashMap<String, String>();
        HashMap<String, String> newvMap = new HashMap<String, String>();
        try {
            reader = new BufferedReader(new FileReader(file));  
            String tempString = null;  
            int line = 1;  
            String last1 = "";
            String last2 = "";
            while ((tempString = reader.readLine()) != null) {  
//                System.out.println("line " + line + ": " + tempString + " <- " + tempString.length() + " " + last1 + " & " + last2);  
                if (tempString.length() == 40) {
                	oldvMap.put(tempString, last2);
                	newvMap.put(tempString, last1);
                	
                }
                line++;  
                last2 = new String(last1);
                last1 = new String(tempString).split("-")[0];
            }  
            reader.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null) {  
                try {  
                    reader.close();  
                } catch (IOException e1) {  
                }  
            }  
        }  
        int line = 1;
        file = new File(fileName);  
        try {
            reader = new BufferedReader(new FileReader(file));  
            String tempString = null;  
            line = 1;
            int init_saved = 1;
            while ((tempString = reader.readLine()) != null) {  
                System.out.println("line " + line + ": " + tempString);  
                String cmt = tempString.split(",")[1];                
                line++;  

                if (cmt.equals("b6d114642733b1b69b58d9b35d5e43521ef93e02"))
                	init_saved = 1;
                if (tempString.split(",")[0].equals("10Luke")
                		|| tempString.split(",")[0].equals("10luke")
                		|| tempString.split(",")[0].equals("27MChat")
                		|| tempString.split(",")[0].equals("3PermissionsSql")
                		|| tempString.split(",")[0].equals("12Skript")
                		|| tempString.split(",")[0].equals("10SimpleClansLite")                		
                		|| tempString.split(",")[0].equals("135CraftBukkit")                		         		
                		|| tempString.split(",")[0].equals("19RacesAndClasses")                  		            		         		
                		|| tempString.split(",")[0].equals("153Faucitt")                  		
                		|| cmt.equals("370690a5ab0c629b436b120dd82e9ba6cd8a267e")
                		|| cmt.equals("039a70b0e103213cc9456de05cdc3f5b1973dab4")
                		|| cmt.equals("35f67554adb1cbea8288b8b64c5275c5b961e6db")
                		|| cmt.equals("666bcaae72eafb2bf9a5bdcdca281933c35276dd")
                		|| cmt.equals("fca32598b8fd9ce4da68a8aefb4cf55b344fb116")
                		|| cmt.equals("5c276c3e2939ee241209a5404426d3f3e5547c27")
                		|| cmt.equals("7825fb118e83e272a3ad65cf7a4244b6689c654d")
                		|| cmt.equals("794fc5ee2a0f7eb43d0eb667d33b48f4a636a2bf")
                		|| cmt.equals("61b47fc43acd900c0c627778eb550487b6d1d7c4")
                		|| cmt.equals("ca9a93c679d96b0e1c5ece0041e101acc0525493")
                		|| cmt.equals("49ed01c19941439af99a486354709d36482464b9")
                		|| cmt.equals("9dc595d620784c801dd61c1e09449d58f401b1db")
                		|| cmt.equals("a00c079ad22f5827bfd5e7fb6dcb13173f30141b")
                		|| cmt.equals("86ba2732481e338dff8f565b7e6f31629ef13ca4")
                		|| cmt.equals("3cc2a91a7634012b58788bcb55d47596d3652347")
                		|| cmt.equals("271a1985c5e09ad9ed1bf5505ca107cea7c6aa57")
                		|| cmt.equals("110ad8c196765c0742313b9bf2923d513531811a")
                		|| cmt.equals("5529aba962ab743253367bfa27aeabc4ee38cf5c")
                		|| cmt.equals("940b49bb4fabeb84cedd48c8107517ff0c9706c7")
                		|| cmt.equals("be11118768d7364c058119edcd62bb7fda379610")//ast_tmp:very complex
                		|| cmt.equals("092c472f117ca18b1500253a5c0303ecb39088dd")
                		|| cmt.equals("836470a835b60ce0bf78eed5727d61bb6754734d")
                		|| cmt.equals("ae20a83b91768523f69dc410b03586188e4dd360")
                		|| cmt.equals("8e8efc9a8dca5bd4561b25a1f90f42a16c7f2972")
                		|| cmt.equals("614bf0b40a2da7f171c935189c8d7be0642d8456")
                		|| cmt.equals("16d7260774ed2e6fa43e4a74b701d2a4ecd571da")
                		|| cmt.equals("ff0c6522b9a7aea43f5d97eb80716013071a426e")
                		|| cmt.equals("e6d45df88e02213f486d463efb92b3530c43f274")
                		|| cmt.equals("136f04dadd207d6dc242f97a854675b41c5edc67")
                		|| cmt.equals("ea686ddbe40fb42aed3ab272f89125b6bc8a63af")
                		|| cmt.equals("2c7cb8c9366e26f647b2c3c6e0900a524748e070")
                		|| cmt.equals("b213be8b5c95404688ace8cc1b8de4352587eb9c")
                		|| cmt.equals("139d60d405119077cde0d04ff23a2b1f43c156e5")
                		|| cmt.equals("0a9ee86d7e35fff6ec9b66115db75cea37b96b03")
                		|| cmt.equals("e935c09b1a6ec6dd4060d34f3aa0819e3e83e13c")
                		|| cmt.equals("c7f36786978ea5aa423be735f586cb233cbaf48f")
                		|| cmt.equals("566a25102046a74ba2f3cbfc67350eadea71236c")
                		|| cmt.equals("8151032ff690ae5906af86f1aaa07bfacf9918ce")
                		|| cmt.equals("4b487822b08ce3306fbc9837116f5f8eab279add")
                		|| cmt.equals("b60e4b698fa5b26bd0f45d5c4f91493b5688e763")
                		|| cmt.equals("fed1bf86163f553ca757dccf77f64a5c4d12676a")
                		|| cmt.equals("14a7f76b7b13d0135edf8fb0438d1048976d924f")
                		|| cmt.equals("5b82003080404dd35b23c34b6ce5afc0b53ee643")
                		|| cmt.equals("7bedd1f806e9ca785f35818d09ee67f8de584d63")
                		|| cmt.equals("ea5109cc39f0a864d27b9677e54bc84697d1325b")
                		|| cmt.equals("5f74cbdddf9c479aed9ea881661b64a5715902fd")
                		|| cmt.equals("0517dae1d5d63d31bf052e19761a59c641acb738")
                		|| cmt.equals("082c96aa684745f7dcdc4790a6064d9eec21d4a6")
                		|| cmt.equals("b6d114642733b1b69b58d9b35d5e43521ef93e02")
                		|| cmt.equals("77c5977996f635d4d2f51f75bc7b728b8f841fea")
                		|| cmt.equals("5189a8ddbf004ca2f35a89c0eee56bb3ac663071") // android
                		|| cmt.equals("d38d632797215a289fd6c65660e2e06ddefcb0c8")
                		|| cmt.equals("9a8a854b3d6f37587489f3b9f052b7d522f02943")
                		|| cmt.equals("a1c510c6dfb5577c2eec2a2f9567f237ec29d056")
                		|| cmt.equals("ceede168ab3796b79bf4f52d0b3a114aa1d61628")
                		|| cmt.equals("9bae772107b3e334e6a96e182cca110a0ca7aace")
                		|| cmt.equals("262be17a093873e96d647bc0f3cf91dd61c26642")
                		|| cmt.equals("e110d88ebdccc39476cc38779a04cffcc255287a")
                		|| cmt.equals("5d851fe3247953177509329b903b8e5ba44e435c")
                		|| cmt.equals("5af599156b3699336739e774db18bc44c77bc754")
                		|| cmt.equals("76c7133f10887ee8e9321d7b51f3c2b97c2737d6")                		
                		|| cmt.equals("b49c3fbd2f50dc4ad985664018a77598baf7020d") //lucene
                		|| cmt.equals("80c145e88e45dd39817a5bca439acbcad0853549")) continue; 
                if (init_saved == 1)
                	one_test(tempString.split(",")[0], cmt , oldvMap.get(cmt), newvMap.get(cmt), lib_name, data_name, lib_check);	
        
//                int k = 0;  
//                while(k!=-1){
//                    k = System.in.read();  
//                    System.out.println(k);  
//                }  
            }  
            CommonValue.flush();
            reader.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
        	System.out.println("Finally:" + line);
            if (reader != null) {  
                try {  
                    reader.close();  
                } catch (IOException e1) {  
                }  
            }  
        }  
    }  
}

//not good example
//folderName = "data_varaha";
//List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//CommitComparatorClient client2 = new CommitComparatorClient("varaha", "lucene");
//client2.analyzeCommitForMigration(cfList, folderName, "3.1.0", "4.4.0");
			
//Skip this because of the lack of lucene API 1.3 
//folderName = "data_mulgara";
//List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//CommitComparatorClient client2 = new CommitComparatorClient("mulgara", "lucene");
//client2.analyzeCommitForMigration(cfList, folderName, "1.3", "2.0.0");

//Complicated Situation
//String old_version = "4.2.0";
//String new_version = "5.3.1";
//String project_name = "cut-and-paste-detector";
//String lib_name = "lucene";
//String commit_brach = "dd91b3";
//Common.common_old_version = lib_name + old_version;
//Common.common_new_version = lib_name + new_version;
//Common.common_project_name = project_name;
//Common.common_commit_number = commit_brach;
//folderName = "data_cut-and-paste-detector";
//List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//CommitComparatorClient client2 = new CommitComparatorClient("cut-and-paste-detector", "lucene");
//client2.analyzeCommitForMigration(cfList, folderName, "4.2.0", "5.3.1");

//Type Migration
//String old_version = "4.7.0";
//String new_version = "4.8.0";
//String commit_brach = "10e0a6";
//String project_name = "musicsearch";
//String lib_name = "lucene";
//Common.common_old_version = lib_name + old_version;
//Common.common_new_version = lib_name + new_version;
//Common.common_project_name = project_name;
//Common.common_commit_number = commit_brach;
//folderName = "data_musicsearch";
//List<ChangeFact> cfList = client.parseChanges("/home/shengzhex/Documents/research_repo/luna_api_migration/test/" + folderName);
//CommitComparatorClient client2 = new CommitComparatorClient("musicsearch", "lucene");
//client2.analyzeCommitForMigration(cfList, folderName, "4.7.0", "4.8.0");