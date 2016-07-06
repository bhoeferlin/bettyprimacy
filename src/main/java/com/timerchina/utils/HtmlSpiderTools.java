package com.timerchina.utils;

import java.io.IOException;
import org.apache.log4j.Logger;
import com.timerchina.spider.bean.SpiderParams;
import com.timerchina.spider.bean.SpiderResult;
import com.timerchina.spider.factory.SpiderFactory;


public class HtmlSpiderTools {
//	public  static Config config = ConfigUtils.getConfig("site");
	private static final int MAX_RETRY_TIMES = 3;
	private static int k = 0;	
//	private static String cookie = config.get("site.cookie");
//	private static String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
//	private static String connection = "keep-alive";
	private static String user_agent = 
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.76 Safari/537.36";
	
	public static String getSpiderHtml(String url, String cookie) {
		return getSpiderResult(url, cookie).getHtml();
	}
	
	
	public static SpiderResult getSpiderResult(String url, String cookie) {
		SpiderParams sp = new SpiderParams();
		sp.setProxy(true);
		sp.setUrl(url);
		sp.setCookie(cookie);
		sp.setUser_agent(user_agent);	
		
		sp.setAccept_language("zh-CN,zh;q=0.8");
		sp.setHttpReadTimeout(50000);
		sp.setHttpConnectionTimeout(50000);
		SpiderResult sr = (SpiderResult) SpiderFactory.execute(sp);
		int responseCode = sr.getResponseCode();
		int count = 0;
		while (responseCode != 200 && count < MAX_RETRY_TIMES) {
			sr = (SpiderResult) SpiderFactory.execute(sp);
			responseCode = sr.getResponseCode();
			count ++;
			k++;
			System.out.println("##重试次数"+count);
		}
		
		if (responseCode == 200) {
			return sr;
		}
		return null;
	}
	
	
	
	public static void main(String[] args) throws IOException {
	    String urlString  = "http://www.baidu.com/s?wd=%E6%8A%A5%E8%80%83%E5%9F%8E%E7%AE%A1%E9%9C%80%E6%9C%AC%E7%A7%91";
	    String string  = getSpiderHtml(urlString, "");
	    System.out.println(string);
	    System.out.println(System.currentTimeMillis());
	    }	 
}



