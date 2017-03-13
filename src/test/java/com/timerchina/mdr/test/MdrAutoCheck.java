package com.timerchina.mdr.test;

import java.util.List;
import java.util.Map;

import com.timerchina.itoolkit.common.db.DBUtil;

public class MdrAutoCheck {
	private static DBUtil db = new DBUtil("jdbc:mysql://101.227.67.231:3306/extract_test?user=spiderman&password=2008rain");

	public static void main(String[] args) {
		String selectSql = "select * from extractor_zheshang_linka";
		List<Map<String, String>> dataMapList = db.executeQuery(selectSql);
		for(int i=0;i<dataMapList.size();i++){
			String id = dataMapList.get(i).get("id");
			String url = dataMapList.get(i).get("url");
			String sourceCode = dataMapList.get(i).get("sourcecode");
			String extractResult = dataMapList.get(i).get("extractresult");
			String simUrlSql = "select sourcecode, extractresult from extractor_zheshang_all where id = '"+ id +"'";
			List<Map<String, String>> simDataMapList = db.executeQuery(simUrlSql);
			String simSourceCode = simDataMapList.get(0).get("sourcecode");
			String simExtractResult = simDataMapList.get(0).get("extractresult");
			if(extractResult==null ) {
//				System.out.println("第一次抽取结果为空：" + id);
				continue;
			}
			else if(simSourceCode == null){
				System.out.println("第二次抽取结果为空：" + id);
			}
			else {
				int length1 = extractResult.length();
				int m = (length1 <= 500)?length1:500;
				int length2 = simExtractResult.length();
				m = (length2 <= m)?length2:m;
				double result = Similarity.SimilarDegree(extractResult.substring(0, m),simExtractResult.substring(0, m));
				if(result < 0.8)
					System.out.println("两次抽取结果不相同：" + id);
			}
		}
	}
}
