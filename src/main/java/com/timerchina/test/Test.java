package com.timerchina.test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.timerchina.itoolkit.common.db.DBUtil;
import com.timerchina.spider.downloader.HttpClientDownloader;
import com.timerchina.spider.pojo.Page;
import com.timerchina.spider.pojo.Request;

public class Test {
	private static DBUtil db = new DBUtil("jdbc:mysql://101.227.67.231:3306/extract_test?user=spiderman&password=2008rain");
	
	public static List<String> read(){
		String path = "sony信源添加0512.xlsx";
		List<String> sourceList = new ArrayList<String>();
		InputStream is;
		try {
			is = new FileInputStream(path);
			Workbook workbook = WorkbookFactory.create(is);
			Sheet sheet = workbook.getSheetAt(0);
			int rowNum = 1;
			Row row = sheet.getRow(rowNum);
			String string = row.getCell(2).toString();
			int len = string.trim().length();
			String siteName = "";
			String forumName = "";
			String url = "";
			String siteNameTemp = row.getCell(0).toString();
			while(len>0){
				try {
					siteName = row.getCell(0).toString();
				} catch (Exception e) {
					siteName = "";
				}
				try {
					forumName = row.getCell(1).toString();
				} catch (Exception e) {
					forumName = "";
				}
				try {
					url = row.getCell(2).toString();
				} catch (Exception e) {
					continue;
				}
				if(siteName.trim().length() == 0){
					siteName = siteNameTemp;
				} 
				else {
					siteNameTemp = siteName;
				}
				String sourceStr = siteName + "##$" + forumName +"##$" + url;
				System.out.println(sourceStr);
				sourceList.add(sourceStr);
				rowNum++;
				row = sheet.getRow(rowNum);
				try {
					string = row.getCell(2).toString();
				} catch (Exception e) {
					string = "";
				}
				len = string.trim().length();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return sourceList;
	}
	
	@org.junit.Test
	public static void insertDB(List<String> sourceList){
		String insertSql = "insert ignore into extractor(sitename,forumname,url) values(?,?,?)";
		for(String string:sourceList){
			String[] strs = string.split("##$");
			String siteName = strs[0];
			String forumName = strs[1];
			String url = strs[2];
			Object[] args = {siteName,forumName,url};
			db.prepareExecuteInsertReturnKey(insertSql, args);
		}
	}
	
	public static void parser(List<String> sourceList){
		String insertSql = "insert ignore into extractor(sitename,forumname,url,sourcecode) values(?,?,?,?)";
		String updateSql = "update extractor set extractresult = ? where url = ?";
		
		for(String string:sourceList){
			String[] strs = string.split("##$");
			String siteName = strs[0];
			String forumName = strs[1];
			String url = strs[2];
			//获取源码并写入数据库
			Request request = new Request();
			request.setUrl(url);
			request.setConnectionTimeout(10000);
			Page page;
			try {
				page = new HttpClientDownloader().download(request);
			} catch (Exception e) {
				continue;
			}
			String content = page.getRawText();
			Object[] args = {siteName,forumName,url,content};
			db.prepareExecuteInsertReturnKey(insertSql, args);
			//解析
			com.timerchina.datarecordparser.MDR lp = new com.timerchina.datarecordparser.MDR();
			String strAll = "";
			for (String z : lp.FindMainDR(content))
				strAll += z;
//			System.out.println(strAll);
			//解析结果写入数据库
			Object[] updateArgs = {strAll,url};
			db.prepareExecuteUpdate(updateSql, updateArgs);
		}
	}
	
	@org.junit.Test
	public void singleTest(){
		Request request = new Request();
		request.setUrl("http://go.evolife.cn/category/tech_95_1.html");
		Page  page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
		System.out.println(content);
		com.timerchina.datarecordparser.MDR lp = new com.timerchina.datarecordparser.MDR();
		String strAll = "";
		for (String z : lp.FindMainDR(content))
			strAll += z + "\r\n";
		System.out.println(strAll);		
	}
	
	@org.junit.Test
	public void reParser(){
		String sqlSql = "select url from extractor20160523 where is_true = -1";
		String updateSql = "update extractor20160523 set sourcecode = ?, extractresult = ? where url = ?";
		List<Map<String, String>> dataList = db.executeQuery(sqlSql);
		for(int i=0;i<dataList.size();i++){
			String url = dataList.get(i).get("url").trim();
			Request request = new Request();
			request.setUrl(url);
			request.setConnectionTimeout(10000);
			Page page;
			try {
				page = new HttpClientDownloader().download(request);
			} catch (Exception e) {
				continue;
			}
			String content = page.getRawText();
			com.timerchina.datarecordparser.MDR lp = new com.timerchina.datarecordparser.MDR();
			String strAll = "";
			for (String z : lp.FindMainDR(content))
				strAll += z;
//			System.out.println(strAll);
			//解析结果写入文件
			Object[] updateArgs = {content,strAll,url};
			db.prepareExecuteUpdate(updateSql, updateArgs);
		}
	}
	
	public static void main(String[] args) {
//		reParser();
	}

}
