package com.timerchina.ptm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.timerchina.itoolkit.common.db.DBUtil;
import com.timerchina.pagetreematch.FinalTemplateGenerator;

public class TemplateMerger {
	private static DBUtil db = new DBUtil("jdbc:mysql://101.227.67.231:3306/extract_test?user=spiderman&password=2008rain");

	public static void main(String[] args) {
		List<String> resultList = new ArrayList<String>();
		String selectSql = "select sitename, regexresult from regex_other where is_can = 1";// and sitename in('电科技','钛媒体')
		List<Map<String, String>> dataList = db.executeQuery(selectSql);
		String sitenamePre = dataList.get(0).get("sitename");
		String regexPre = dataList.get(0).get("regexresult");
		List<String> regexList = (List<String>) JSON.parse(regexPre);
		ArrayList<List<String>> regexDataList = new ArrayList<List<String>>();
		regexDataList.add(regexList);
//		regexDataList.add(regexList);
		for(int i = 1; i < dataList.size(); i++){
			String sitename = dataList.get(i).get("sitename");
			String regex = dataList.get(i).get("regexresult");
			regexPre = regex;
			regexList = (List<String>) JSON.parse(regexPre);
			if(sitename.equals(sitenamePre)) regexDataList.add(regexList);
			else {
				System.out.println("当前合并站点："+sitenamePre);
				if(regexDataList.size() == 0) regexDataList.add(regexList);
				FinalTemplateGenerator ftg = new FinalTemplateGenerator();
				resultList = ftg.outerIterUnmatchProcess(regexDataList);
				if(resultList == null){
					System.out.println("合并失败！");
				} 
				System.out.println(resultList);
				String result = JSON.toJSONString(resultList);
				//更新到数据库
				String updateSql = "insert into template_other(template,sitename) values('"+result+"','"+sitenamePre+"')";
				db.executeUpdate(updateSql);
				regexDataList = new ArrayList<List<String>>();
				regexDataList.add(regexList);
			}
			sitenamePre = sitename;
		}
	}
	@Test
	public void mergeTest(){
		List<String> resultList = new ArrayList<String>();
		String selectSql = "select sitename, regexresult from regex_other where is_can = 1 and id <= 7";
		List<Map<String, String>> dataList = db.executeQuery(selectSql);
		ArrayList<List<String>> regexDataList = new ArrayList<List<String>>();
		for(int i = 0; i < dataList.size(); i++){
			String regex = dataList.get(i).get("regexresult");
			List<String> regexList = (List<String>) JSON.parse(regex);
			regexDataList.add(regexList);
		}
		FinalTemplateGenerator ftg = new FinalTemplateGenerator();
		resultList = ftg.outerIterUnmatchProcess(regexDataList);
		if(resultList == null){
			System.out.println("合并失败！");
		} 
		System.out.println();
		System.out.println(resultList);
	}
}
