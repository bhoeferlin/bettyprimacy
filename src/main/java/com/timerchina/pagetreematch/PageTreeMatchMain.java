package com.timerchina.pagetreematch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.timerchina.itoolkit.common.db.DBUtil;

public class PageTreeMatchMain {
	private static DBUtil db = new DBUtil("jdbc:mysql://101.227.67.231:3306/extract_test?user=spiderman&password=2008rain");

	public List<String> execute(String str){
		List<String> resultList = new ArrayList<String>();
		
		PrimaryTemplateGenerator hnp = new PrimaryTemplateGenerator();
		ArrayList<List<String>> fragmentLists = hnp.buildPrimaryTemplate(str);
		if(fragmentLists == null||fragmentLists.size() == 0){
			System.out.println("初始模板构建失败！");
			return null;
		}
		System.out.println("初始模板生成");
		FinalTemplateGenerator ftg = new FinalTemplateGenerator();
		resultList = ftg.outerIterUnmatchProcess(fragmentLists);
		if(resultList == null){
			return null;
		} 
		return resultList;
	}
	public static void main(String[] args) {
		String selectSql = "select extractresult from regex_other where id = 3";
		List<Map<String, String>> dataMapList = db.executeQuery(selectSql);
		String content = dataMapList.get(0).get("extractresult");
				
		PageTreeMatchMain ptm = new PageTreeMatchMain();
		long start = System.currentTimeMillis();
		List<String> resultList = ptm.execute(content);
		long end = System.currentTimeMillis();
		System.out.println("共用时："+(end - start)+"ms");
		if(resultList != null)
			System.out.println(resultList);
		else {
			System.out.println("抽取失败！");
		}
	}
}
