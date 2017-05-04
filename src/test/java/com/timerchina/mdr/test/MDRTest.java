package com.timerchina.mdr.test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;

import com.timerchina.datarecord.parser.NodeData;
import com.timerchina.datarecord.parser.Tree;
import com.timerchina.itoolkit.common.db.DBUtil;
import com.timerchina.spider.downloader.HttpClientDownloader;
import com.timerchina.spider.pojo.Page;
import com.timerchina.spider.pojo.Request;

public class MDRTest {
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
//				System.out.println(sourceStr);
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
	
	public static void insertDB(List<String> sourceList){
		String insertSql = "insert ignore into extractor011(sitename,forumname,url) values(?,?,?)";
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
			String[] strs = string.split("\\#\\#\\$");
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
			long start = System.currentTimeMillis();
			com.timerchina.datarecord.parser.MDR lp = new com.timerchina.datarecord.parser.MDR();
			String strAll = "";
			for (String z : lp.FindMainDR(content))
				strAll += z;
			long end = System.currentTimeMillis();
			System.out.println("解析共用时："+(end-start));
//			System.out.println(strAll);
			//解析结果写入数据库
			Object[] updateArgs = {strAll,url};
			db.prepareExecuteUpdate(updateSql, updateArgs);
		}
	}
	/**
	 * 获取源码解析并写入数据库
	 * **/
	@Test
	public void getSourceCodeAndParser(){
		String selectSql = "select id,url from regex_other";//is_true = 1
//		String insertSql = "update extractor set sourcecode = ? where id = ?";
		String updateSql = "update regex_other set extractresult = ? where id = ?";
		
		List<Map<String, String>> dataMapList = db.executeQuery(selectSql);
		for(int i = 0; i < dataMapList.size(); i++){
			String id = dataMapList.get(i).get("id");
			String url = dataMapList.get(i).get("url");
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
//			Object[] args = {content,id};
//			db.prepareExecuteInsertReturnKey(insertSql, args);
			//解析
			System.out.println("当前运行id:"+id);
			long start = System.currentTimeMillis();
			com.timerchina.datarecord.parser.MDR2 lp = new com.timerchina.datarecord.parser.MDR2();
			String strAll = "";
			for (String z : lp.fetchMainDR(content))
				strAll += z;
			long end = System.currentTimeMillis();
			System.out.println("解析共用时："+(end-start));
//			System.out.println(strAll);
			//解析结果写入数据库
			Object[] updateArgs = {strAll,id};
			db.prepareExecuteUpdate(updateSql, updateArgs);
		}
	}
	/**
	 *输出最终模块（文本长度作为筛选）
	 **/
	@Test
	public void singleTest(){
		Request request = new Request();
		request.setUrl("http://www.znds.com/tv-716976-1-1.html");
		Page  page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
//		System.out.println(content);
		long start = System.currentTimeMillis();
		com.timerchina.datarecord.parser.MDR2 lp = new com.timerchina.datarecord.parser.MDR2();
		String strAll = "";
		for (String z : lp.fetchMainDR(content))
			strAll += z + "\r\n";
		long end = System.currentTimeMillis();
		System.out.println("共用时：" + (end-start) + "ms");
		System.out.println(strAll);		
	}
	/**
	 *输出所有模块 
	 **/
	@Test
	public void singleMultiTest(){
		Request request = new Request();
		request.setUrl("http://sz.58.com/chuzu/");
		Page page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
//		System.out.println(content);
		com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
		String strAll = "";
		Tree tree = lp.genDOMTree(content);
		for (NodeData[] z : lp.fetchMainDR(tree))
//			strAll += z + "\r\n";
//		System.out.println(strAll);
		{
			System.out.println("***********************************");
			for(NodeData s:z){
				System.out.println("parentIndex:" + s.getParentIndex());
				System.out.println("childIndex:" + s.getChildIndex());
				System.out.println(s.getNodeData() + "\r\n");
			}
		}
	}
	/**
	 *输出最终模块（在新的条件下） 
	 **/
	@org.junit.Test
	public void singleModifyTest(){
		Request request = new Request();
		request.setUrl("http://www.bocichina.com/boci/asset/cms/checkPage.jsp?whichCat=zcgl_jhlc_cpgg&forthMenu=qtcd_asset_jhlc_one_ggxx");
		Page  page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
		content = content.replaceAll("<!--[\\s\\S]*?-->", "");
		content = content.replaceAll("<script>[\\s\\S]*?</script>", "");
//		System.out.println(content);
		com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
		String strAll = "";
		for (NodeData[] z : lp.fetchModifyMainDR(content))
//			strAll += z + "\r\n";
//		System.out.println(strAll);
		{
			System.out.println("***********************************");
			for(NodeData s:z){
				System.out.println(s.getNodeData() + "\r\n");
			}
		}
	}
	/**
	 * 写入到数据库 
	 **/
	@Test
	public void reParser(){
		String sqlSql = "select id,url from extractor where id = 76";
		String updateSql = "update extractor set sourcecode = ?, extractresult = ? where id = ?";
		List<Map<String, String>> dataList = db.executeQuery(sqlSql);
		for(int i=0;i<dataList.size();i++){
			int id = Integer.parseInt(dataList.get(i).get("id"));
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
			content = content.replaceAll("<!--[\\s\\S]*?-->", "");
			content = content.replaceAll("<script>[\\s\\S]*?</script>", "");
			com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
			String strAll = "";
			for (NodeData[] z: lp.fetchModifyMainDR(content))
			{
				for(NodeData s:z){
					strAll += s.getNodeData();
				}
			}
//			System.out.println(strAll);
			//解析结果写入数据库
			Object[] updateArgs = {content,strAll,id};
			db.prepareExecuteUpdate(updateSql, updateArgs);
		}
	}
	/**
	 * 输出Tree
	 **/
	@org.junit.Test
	public void outputTree(){
		Request request = new Request();
		request.setUrl("https://www.baidu.com/s?wd=%E6%AD%A3%E5%88%99&rsv_spt=1&rsv_iqid=0xfd20fefc0001fab5&issp=1&f=8&rsv_bp=0&rsv_idx=2&ie=utf-8&tn=baiduhome_pg&rsv_enter=1&rsv_sug3=6&rsv_sug1=3&rsv_sug7=101");
		Page  page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
		com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
		Tree tree = lp.genDOMTree(content);
		tree.Output();
//		System.out.println(tree.childParentMap.get(1977));
	}
	
	/**
	 * 计算两个DataRecord的相似度
	 **/
	@org.junit.Test
	public void simCalculate(){
		String html1 = "<tr>  <td>2016-01-21</td>  <td>35</td>  <td>6.418%</td>  <td>2016-01-21</td> </tr><tr>  <td>2016-02-25</td>  <td>35</td>  <td>6.559%</td>  <td>2016-02-25</td> </tr><tr>  <td>2016-03-17</td>  <td>21</td>  <td>6.399%</td>  <td>2016-03-17</td> </tr><tr>  <td>2016-04-21</td>  <td>35</td>  <td>5.960%</td>  <td>2016-04-21</td> </tr><tr>  <td>2016-05-19</td>  <td>28</td>  <td>5.418%</td>  <td>2016-05-19</td> </tr><tr>  <td>2016-06-16</td>  <td>28</td>  <td>5.726%</td>  <td>2016-06-16</td> </tr>";
		String html2 = "<tr> <td>2016-03-22</td> <td>1.0804</td> <td>3.618%</td> <td>2016-03-22</td></tr><tr> <td>2016-03-23</td> <td>1.0393</td> <td>3.631%</td> <td>2016-03-23</td></tr><tr> <td>2016-03-24</td> <td>0.9970</td> <td>3.657%</td> <td>2016-03-24</td></tr><tr> <td>2016-03-25</td> <td>0.9999</td> <td>3.650%</td> <td>2016-03-25</td></tr><tr> <td>2016-03-26</td> <td>0.7036</td> <td>3.669%</td> <td>2016-03-26</td></tr><tr> <td>2016-03-27</td> <td>0.6071</td> <td>3.691%</td> <td>2016-03-27</td></tr><tr> <td>2016-03-28</td> <td>1.5262</td> <td>3.692%</td> <td>2016-03-28</td></tr><tr> <td>2016-03-29</td> <td>1.1227</td> <td>3.715%</td> <td>2016-03-29</td></tr><tr> <td>2016-03-30</td> <td>1.1253</td> <td>3.761%</td> <td>2016-03-30</td></tr><tr> <td>2016-03-31</td> <td>1.0828</td> <td>3.808%</td> <td>2016-03-31</td></tr><tr> <td>2016-04-01</td> <td>0.9939</td> <td>3.805%</td> <td>2016-04-01</td></tr><tr> <td>2016-04-02</td> <td>0.7950</td> <td>3.854%</td> <td>2016-04-02</td></tr><tr> <td>2016-04-03</td> <td>0.6898</td> <td>3.899%</td> <td>2016-04-03</td></tr><tr> <td>2016-04-04</td> <td>0.6897</td> <td>3.447%</td> <td>2016-04-04</td></tr><tr> <td>2016-04-05</td> <td>1.5744</td> <td>3.691%</td> <td>2016-04-05</td></tr><tr> <td>2016-04-06</td> <td>1.0713</td> <td>3.661%</td> <td>2016-04-06</td></tr><tr> <td>2016-04-07</td> <td>1.0504</td> <td>3.644%</td> <td>2016-04-07</td></tr><tr> <td>2016-04-08</td> <td>0.9570</td> <td>3.624%</td> <td>2016-04-08</td></tr><tr> <td>2016-04-09</td> <td>0.7209</td> <td>3.584%</td> <td>2016-04-09</td></tr><tr> <td>2016-04-10</td> <td>0.6215</td> <td>3.547%</td> <td>2016-04-10</td></tr><tr> <td>2016-04-11</td> <td>1.3511</td> <td>3.905%</td> <td>2016-04-11</td></tr><tr> <td>2016-04-12</td> <td>1.0546</td> <td>3.624%</td> <td>2016-04-12</td></tr><tr> <td>2016-04-13</td> <td>0.9485</td> <td>3.557%</td> <td>2016-04-13</td></tr><tr> <td>2016-04-14</td> <td>0.9083</td> <td>3.481%</td> <td>2016-04-14</td></tr><tr> <td>2016-04-15</td> <td>0.9112</td> <td>3.456%</td> <td>2016-04-15</td></tr><tr> <td>2016-04-16</td> <td>0.6735</td> <td>3.430%</td> <td>2016-04-16</td></tr><tr> <td>2016-04-17</td> <td>0.5690</td> <td>3.402%</td> <td>2016-04-17</td></tr><tr> <td>2016-04-18</td> <td>1.3164</td> <td>3.383%</td> <td>2016-04-18</td></tr><tr> <td>2016-04-19</td> <td>1.0277</td> <td>3.369%</td> <td>2016-04-19</td></tr><tr> <td>2016-04-20</td> <td>0.9545</td> <td>3.372%</td> <td>2016-04-20</td></tr><tr> <td>2016-04-21</td> <td>0.9237</td> <td>3.380%</td> <td>2016-04-21</td></tr><tr> <td>2016-04-22</td> <td>0.9573</td> <td>3.405%</td> <td>2016-04-22</td></tr><tr> <td>2016-04-23</td> <td>0.6597</td> <td>3.398%</td> <td>2016-04-23</td></tr><tr> <td>2016-04-24</td> <td>0.5704</td> <td>3.399%</td> <td>2016-04-24</td></tr><tr> <td>2016-04-25</td> <td>1.4019</td> <td>3.445%</td> <td>2016-04-25</td></tr><tr> <td>2016-04-26</td> <td>1.0041</td> <td>3.432%</td> <td>2016-04-26</td></tr><tr> <td>2016-04-27</td> <td>0.9754</td> <td>3.443%</td> <td>2016-04-27</td></tr><tr> <td>2016-04-28</td> <td>0.9319</td> <td>3.448%</td> <td>2016-04-28</td></tr><tr> <td>2016-04-29</td> <td>0.9561</td> <td>3.447%</td> <td>2016-04-29</td></tr><tr> <td>2016-04-30</td> <td>0.6222</td> <td>3.427%</td> <td>2016-04-30</td></tr><tr> <td>2016-05-01</td> <td>0.5427</td> <td>3.412%</td> <td>2016-05-01</td></tr><tr> <td>2016-05-02</td> <td>0.5427</td> <td>2.950%</td> <td>2016-05-02</td></tr><tr> <td>2016-05-03</td> <td>1.7257</td> <td>3.338%</td> <td>2016-05-03</td></tr><tr> <td>2016-05-04</td> <td>1.0391</td> <td>3.372%</td> <td>2016-05-04</td></tr><tr> <td>2016-05-05</td> <td>0.9833</td> <td>3.400%</td> <td>2016-05-05</td></tr><tr> <td>2016-05-06</td> <td>0.8905</td> <td>3.364%</td> <td>2016-05-06</td></tr><tr> <td>2016-05-07</td> <td>0.6026</td> <td>3.354%</td> <td>2016-05-07</td></tr><tr> <td>2016-05-08</td> <td>0.5079</td> <td>3.335%</td> <td>2016-05-08</td></tr><tr> <td>2016-05-09</td> <td>1.4167</td> <td>3.807%</td> <td>2016-05-09</td></tr><tr> <td>2016-05-10</td> <td>0.9857</td> <td>3.407%</td> <td>2016-05-10</td></tr><tr> <td>2016-05-11</td> <td>0.9545</td> <td>3.362%</td> <td>2016-05-11</td></tr><tr> <td>2016-05-12</td> <td>0.9174</td> <td>3.326%</td> <td>2016-05-12</td></tr><tr> <td>2016-05-13</td> <td>0.9159</td> <td>3.340%</td> <td>2016-05-13</td></tr><tr> <td>2016-05-14</td> <td>0.6022</td> <td>3.340%</td> <td>2016-05-14</td></tr><tr> <td>2016-05-15</td> <td>0.5220</td> <td>3.347%</td> <td>2016-05-15</td></tr><tr> <td>2016-05-16</td> <td>1.4518</td> <td>3.366%</td> <td>2016-05-16</td></tr><tr> <td>2016-05-17</td> <td>0.9379</td> <td>3.340%</td> <td>2016-05-17</td></tr><tr> <td>2016-05-18</td> <td>0.8867</td> <td>3.304%</td> <td>2016-05-18</td></tr><tr> <td>2016-05-19</td> <td>0.8727</td> <td>3.280%</td> <td>2016-05-19</td></tr><tr> <td>2016-05-20</td> <td>0.8524</td> <td>3.246%</td> <td>2016-05-20</td></tr><tr> <td>2016-05-21</td> <td>0.5262</td> <td>3.205%</td> <td>2016-05-21</td></tr><tr> <td>2016-05-22</td> <td>0.4557</td> <td>3.169%</td> <td>2016-05-22</td></tr><tr> <td>2016-05-23</td> <td>1.3874</td> <td>3.134%</td> <td>2016-05-23</td></tr><tr> <td>2016-05-24</td> <td>0.9049</td> <td>3.117%</td> <td>2016-05-24</td></tr><tr> <td>2016-05-25</td> <td>0.8468</td> <td>3.095%</td> <td>2016-05-25</td></tr><tr> <td>2016-05-26</td> <td>0.8454</td> <td>3.080%</td> <td>2016-05-26</td></tr><tr> <td>2016-05-27</td> <td>0.8779</td> <td>3.094%</td> <td>2016-05-27</td></tr><tr> <td>2016-05-28</td> <td>0.5007</td> <td>3.080%</td> <td>2016-05-28</td></tr><tr> <td>2016-05-29</td> <td>0.4336</td> <td>3.069%</td> <td>2016-05-29</td></tr><tr> <td>2016-05-30</td> <td>1.4214</td> <td>3.087%</td> <td>2016-05-30</td></tr><tr> <td>2016-05-31</td> <td>0.9156</td> <td>3.093%</td> <td>2016-05-31</td></tr><tr> <td>2016-06-01</td> <td>0.8947</td> <td>3.118%</td> <td>2016-06-01</td></tr><tr> <td>2016-06-02</td> <td>0.8505</td> <td>3.121%</td> <td>2016-06-02</td></tr><tr> <td>2016-06-03</td> <td>0.8460</td> <td>3.104%</td> <td>2016-06-03</td></tr><tr> <td>2016-06-04</td> <td>0.5377</td> <td>3.124%</td> <td>2016-06-04</td></tr><tr> <td>2016-06-05</td> <td>0.4620</td> <td>3.139%</td> <td>2016-06-05</td></tr><tr> <td>2016-06-06</td> <td>1.4056</td> <td>3.131%</td> <td>2016-06-06</td></tr><tr> <td>2016-06-07</td> <td>0.9227</td> <td>3.134%</td> <td>2016-06-07</td></tr><tr> <td>2016-06-08</td> <td>0.8850</td> <td>3.129%</td> <td>2016-06-08</td></tr><tr> <td>2016-06-09</td> <td>0.5821</td> <td>2.985%</td> <td>2016-06-09</td></tr><tr> <td>2016-06-10</td> <td>0.5021</td> <td>2.800%</td> <td>2016-06-10</td></tr><tr> <td>2016-06-11</td> <td>0.5020</td> <td>2.781%</td> <td>2016-06-11</td></tr><tr> <td>2016-06-12</td> <td>0.5020</td> <td>2.803%</td> <td>2016-06-12</td></tr><tr> <td>2016-06-13</td> <td>2.0358</td> <td>3.141%</td> <td>2016-06-13</td></tr><tr> <td>2016-06-14</td> <td>1.0965</td> <td>3.235%</td> <td>2016-06-14</td></tr><tr> <td>2016-06-15</td> <td>1.0353</td> <td>3.316%</td> <td>2016-06-15</td></tr><tr> <td>2016-06-16</td> <td>0.9614</td> <td>3.520%</td> <td>2016-06-16</td></tr><tr> <td>2016-06-17</td> <td>0.9056</td> <td>3.738%</td> <td>2016-06-17</td></tr><tr> <td>2016-06-18</td> <td>0.6382</td> <td>3.812%</td> <td>2016-06-18</td></tr><tr> <td>2016-06-19</td> <td>0.5558</td> <td>3.841%</td> <td>2016-06-19</td></tr><tr> <td>2016-06-20</td> <td>1.4142</td> <td>3.505%</td> <td>2016-06-20</td></tr>";
		com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
		Tree tr1 = lp.genDOMTree(html1);
		Tree tr2 = lp.genDOMTree(html2);
		double result = lp.Similarity(tr1, 0, tr2, 0);
		System.out.println(result);
	}
	
	public static void main(String[] args) {
		Request request = new Request();
		request.setUrl("http://trust.ecitic.com/XXPL_JZPL/detail.jsp?vc_code=1&vc_name=星石8期");
		Page  page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
		com.timerchina.datarecord.parser.MDR3 lp = new com.timerchina.datarecord.parser.MDR3();
		Tree tree = lp.genDOMTree(content);
		tree.generateIndexMap();
		System.out.println(tree.childParentMap.get(660));
	}
}
