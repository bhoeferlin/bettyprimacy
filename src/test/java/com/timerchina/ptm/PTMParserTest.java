package com.timerchina.ptm;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.timerchina.excel.ExcelWriter;
import com.timerchina.itoolkit.common.db.DBUtil;
import com.timerchina.pagetreematch.HtmlProcessUtils;
import com.timerchina.pagetreematch.PrimaryTemplateGenerator;
import com.timerchina.pagetreematch.parser.PTMParser;

public class PTMParserTest {
	private static DBUtil db = new DBUtil("jdbc:mysql://101.227.67.231:3306/extract_test?user=spiderman&password=2008rain");
	private static String titles = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28";
	private static ExcelWriter writer;
	
//	static {
//		writer = new ExcelWriter(titles.split(","));
//	}
	public static void execute(String userDir){
		String regexSql = "select sitename,template from template_other";
		List<Map<String, String>> regexDataMapList = db.executeQuery(regexSql);
		Map<String, List<String>> templateMap = new HashMap<String, List<String>>();
	/*
		for(int j = 0; j < regexDataMapList.size(); j++){
			String sitename = regexDataMapList.get(j).get("sitename");
			String template = regexDataMapList.get(j).get("template");
			List<String> regexList = (List<String>) JSON.parse(template);
			templateMap.put(sitename, regexList);
		}
	*/	
		String selectSql = "select id, sitename, forumname, extractresult,regexresult from regex_other where is_can = 1 and id = 3";
		List<Map<String, String>> dataMapList = db.executeQuery(selectSql);
		for(int i = 0; i < dataMapList.size(); i++){
			writer = new ExcelWriter(titles.split(","));
			String id = dataMapList.get(i).get("id");
			String extractor = dataMapList.get(i).get("extractresult");
			String sitename = dataMapList.get(i).get("sitename");
			String forumname = dataMapList.get(i).get("forumname");
			String fullFile = userDir + File.separator + "extractresult/other/易车网/" + id + "_"+ sitename + "_" + forumname;
	
			String regex = dataMapList.get(i).get("regexresult");
			@SuppressWarnings("unchecked")
			List<String> regexList = (List<String>) JSON.parse(regex);
	
			System.out.println("当前运行id:"+id);
			long start = System.currentTimeMillis();
			List<String> extractorList = HtmlProcessUtils.normalize(extractor);
			List<Map<String, String>> extractResultList = PTMParser.process(regexList, extractorList);
			long end = System.currentTimeMillis();
			System.out.println("解析共用时："+(end - start)+"ms");
			writer.appendSheetMap(extractResultList).export(fullFile);
//			for(Map<String, String> map: extractResultList)
//				System.out.println(map);
		}
	}
	
	public static void main(String[] args) {
		String userDir = System.getProperty("user.dir");
		execute(userDir);
	}
}
