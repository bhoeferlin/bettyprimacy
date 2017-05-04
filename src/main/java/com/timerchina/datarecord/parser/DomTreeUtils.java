package com.timerchina.datarecord.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomTreeUtils {
	/**
	 * 统计字符串中标签的个数
	 * @param 
	 **/
	public static int numTag(String string){
		int countTag = 0;
		Pattern regex = Pattern.compile("</\\w+>");
		Matcher matcher = regex.matcher(string);

		while (matcher.find()) {
			countTag += 1;
		}
		return countTag;
	}
	/**
	 * 从中文中抽取出英文单词、数字
	 **/
	public static List<String> getEnglishWord(String text) {
		ArrayList<String> tempList = new ArrayList<String>();
		String temp = "";
		if (text.trim().length() != 0) {
			try {
				Pattern regex = Pattern.compile("[a-zA-Z]+");// 匹配英文
				Matcher matcher = regex.matcher(text);

				while (matcher.find()) {
					temp = matcher.group(0);
					tempList.add(temp);
				}
			} catch (Exception e) {
			}
			try {
				Pattern regex = Pattern.compile("\\d+");// 匹配数字
				Matcher matcher = regex.matcher(text);

				while (matcher.find()) {
					temp = matcher.group(0);
					tempList.add(temp);
				}
			} catch (Exception e) {
			}
		}
		return tempList;
	}

	/**
	 * 统计汉字的长度:一串英文算一个，以标点符号分开
	 **/
	public static int getChineseLength(String string) {
		string = string.replaceAll("([\\pP\\p{Punct}])", " ");
		int length = 0;
		List<String> tempList = getEnglishWord(string);
		string = string.replaceAll("[a-zA-Z]+", "");
		// string = string.replaceAll("\\d+", "");
		length = string.trim().length() + tempList.size();
		return length;
	}
	/**
	 * 统计字符串中超链接的个数
	 * @param 
	 **/
	public static int numHyperlink(String string){
		int countA = 0;
//		for(int i = 0; i <string.length; i++){
//			String str = string[i];
			Pattern regex = Pattern.compile("<.*?href.*?>|.*?/a>");
			Matcher matcher = regex.matcher(string);
			
			while(matcher.find()){
				countA += 1;
			}
			
//		}
		return countA;
	}

	public static void main(String[] args) {
		String string = "<tr>  <th>成立规模</th>  <td>20000 万元</td> </tr><tr>  <th>投资起点</th>  <td>100 万元</td> </tr><tr>  <th>信托期限</th>  <td>48月</td> </tr><tr>  <th>收&nbsp;益&nbsp;率</th>  <td> 浮动收益 </td> </tr>";
		int feature = string.replaceAll("\\s+", "").replaceAll("<.*?>", "").length();
		int c = numHyperlink(string);
		float linkaDensity = (float)c/(feature + 1);
		System.out.println(linkaDensity);
	}
}
