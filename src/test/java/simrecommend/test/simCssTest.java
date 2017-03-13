package simrecommend.test;

import org.apache.log4j.Logger;

import com.timerchina.spider.downloader.HttpClientDownloader;
import com.timerchina.spider.pojo.Page;
import com.timerchina.spider.pojo.Request;
import com.timerchina.utils.CommonTools;

public class simCssTest {
	protected static Logger logger = CommonTools.getLogger();

	public static void main(String[] args) {
		String url = "http://bbs.pcauto.com.cn/topic-12858336.html";
		String css = "body > #wrapper > #wrapper_wrapper > #container > #content_left > #3";
		
		String content = "";
		Request request = new Request();
		request.setUrl(url);
		Page page;
		try {
			page = new HttpClientDownloader().download(request);
			content = page.getRawText();
			System.out.println(content);
//			Document document = Jsoup.parse(content);
			long start = System.currentTimeMillis();
//			String simCss = CssRecommend.getSimCsses(document, css);
			long end = System.currentTimeMillis();
			logger.info("共用时："+(end-start)+"ms");
//			logger.info(simCss);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
