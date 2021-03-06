package com.timerchina.pagetreematch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HtmlProcessUtils {
	/**
	 * 规格化处理：
	 * 逐行读入HTML片段，如果读入的HTML代码是“<”或者“>”时，就输入一个换行符；否则，就直接写入；把连续出现两个换行符的行删掉一个。
	 **/
	public static List<String> normalize(String content){
		content = content.replaceAll("<br>", "").replace("&nbsp;", "");
		content = content.replaceAll("<!--.*?-->", "");		//去除注释
		List<String> result = new ArrayList<String>();
		content = content.replaceAll("\\<", "\n\\<");
		content = content.replaceAll("\\>", "\\>\n");
		String[] lineData = content.split("\n");
		for(String str:lineData){
			if(str.trim().length() == 0) continue;
			if(!HtmlProcessUtils.isText(str)) continue;		//去除符号
			else if(HtmlProcessUtils.isStr(str)){
				result.add(str);
			}else {
				String tagName = str.split("\\s+")[0];
				if(tagName.contains("<")&&!tagName.endsWith(">")) tagName = tagName + ">";
				result.add(tagName);
			}
		}
		return result;
	}
	/**
	 * 是否为字符串
	 * */ 
	public static boolean isStr(String text) {
		if (text.trim().length() != 0) {
			try {
				Pattern regex = Pattern.compile("[</|<].*?>");
				Matcher matcher = regex.matcher(text);

				if (matcher.find())
					return false;
			} catch (Exception e) {
			}
		}
		return true;
	}
	/**
	 * 是否为中文
	 * */ 
	public static boolean isChinese(String text) {
		if (text.trim().length() == 0) return true;
		else {
			try {
				Pattern regex = Pattern.compile("[\u4E00-\u9FA5]");// 匹配汉字
				Matcher matcher = regex.matcher(text);

				if (matcher.find())
					return true;
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 * 删除list指定元素
	 * */ 
	public static List<String> delListByElement(List<String> template, String element) {
		// 去除所有的占位符
		for (int i = 0; i < template.size(); i++) {
			if (template.get(i).equals(element)) {
				template.remove(i);
				i--;
			}
		}
		return template;
	}
	/**
	 * 删除连续的迭代符(字段可选只留一个)
	 * */ 
	public static void delPairTag(List<String> template) {
		if (template == null || template.size() == 0)
			return;
		String dataPre = getTagWithEnder(template.get(0));
		String data = "";
		String dataPost = "";
		for (int i = 1; i < template.size() - 1; i++) {
			data = template.get(i);
			dataPost = getTagWithEnder(template.get(i + 1));
			if (data.equals("#PCDATA?")&&(dataPost.equals("/" + dataPre))) {
				template.remove(i - 1);
				template.remove(i); 
				i = i - 1;
			}
			dataPre = getTagWithEnder(data);
		}
	}
	/**
	 * 删除连续的迭代符(标签可选只留一个)
	 * */ 
	public static void delListRepeatElementMore(List<String> template) {
		if(template==null||template.size()==0) return;
		String dataPre = template.get(0);
		String data = "";
		int index = 0;
		for(int i = 1; i < template.size(); i++){
			data = template.get(i);
			if(dataPre.contains("?")&&data.contains("?")){
				if(dataPre.contains("<S?>")) index = i - 1;
				else index = i;
				template.remove(index);
				i --;
			} 
			dataPre = data;
		}
	}
	/**
	 * 删除连续的迭代符
	 * */ 
	public static List<String> delListRepeatElement(List<String> template,String element){
		if(template==null||template.size()==0) return null;
		String dataPre = template.get(0);
		String data = "";
		for(int i = 1; i < template.size(); i++){
			data = template.get(i);
			if(data.equals(dataPre)&&data.equals(element)){
				template.remove(i);
				i --;
			} 
			//删除连续的#PCDATA和#PCDATA?
			if(dataPre.equals("#PCDATA")&&data.equals(element)){
				template.remove(i - 1);
				i --;
			}
			else if(dataPre.equals("#PCDATA?")&&data.equals("#PCDATA")){
				template.remove(i);
				i --;
			}
			dataPre = data;
		}
		return template;
	}
	
	/**
	 * 删除指定上下界的元素，直接删除
	 * */ 
	public static List<String> delListByIndexDirect(List<String> template,int lower, int upper) {
		int delta = upper - lower + 1;
		int index = lower;
		for (int i = 0; i < delta; i++) {
			template.remove(index);
		}
		return template;
	}
	/**
	 * 删除指定上下界的元素,用填充
	 * */ 
	public static List<String> delListByIndex(List<String> template, int lower,
			int upper) {
		for (int i = lower; i <= upper; i++) {
			template.set(i, "placeholder");
		}
		return template;
	}
	
	/**
	 * list深拷贝
	 * */ 
	@SuppressWarnings("unchecked")
	public static List<String> deepCopy(List<String> src) {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(byteOut);
			out.writeObject(src);
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<String> dest = new ArrayList<>();
		ByteArrayInputStream byteIn = new ByteArrayInputStream(
				byteOut.toByteArray());
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(byteIn);
			dest = (List<String>) in.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return dest;
	}
	/**
	 * 判断包含中英文、数字的文本
	 * */
	public static boolean isText(String string){
		boolean flag = false;
		//匹配中文
		flag = isChinese(string);
		if(flag == true) return flag;
		//匹配字符
		if (string.trim().length() != 0) {
			try {
				Pattern regex = Pattern.compile("[a-zA-Z]+");//匹配英文
				Matcher matcher = regex.matcher(string);

				if (matcher.find()) return true;
			} catch (Exception e) {
			}
		}
		//匹配数字
		if (string.trim().length() != 0) {
			try {
				Pattern regex = Pattern.compile("\\d+");//匹配数字
				Matcher matcher = regex.matcher(string);

				if (matcher.find()) return true;
			} catch (Exception e) {
			}
		}
		return flag;
	}
	/**
	 * 获取标签名
	 * */
	public static String getTag(String data){
		if(HtmlProcessUtils.isStr(data))
			return data;
		String tag = "";
		try {
			Pattern regex = Pattern.compile("[</|<](\\w+)>");
			Matcher matcher = regex.matcher(data);
			
			while(matcher.find()){
				tag = matcher.group(1);
			}
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return tag;
	}
	/**
	 * 获取标签名,包括标签结束符
	 * */
	public static String getTagWithEnder(String data) {
		if (HtmlProcessUtils.isStr(data))
			return data;
		String tag = "";
		try {
			Pattern regex = Pattern.compile("<([/|\\w]+)>");
			Matcher matcher = regex.matcher(data);

			while (matcher.find()) {
				tag = matcher.group(1);
			}
		} catch (PatternSyntaxException ex) {
			ex.printStackTrace();
		}
		return tag;
	}
 
	/**
	 * 判断是否包含set集合内元素
	 * */
	public static boolean isContainWord(String str, Set<String> wordSet){
		boolean flag = false;
		str = str.toLowerCase();
		for(String word:wordSet)
			flag = flag||str.contains(word);
		return flag;
	}
	/**
	 * 向后取list中n个元素
	 * */
	public static String getNElement(List<String> sourceCodeList, int index, int n){
		String string = "";
		for(int i = index; i < index + n; i++){
			if(i >= sourceCodeList.size()) return string;
			string += sourceCodeList.get(i).replace("(", "").replace(")+", "").replace("?", "");
		}
		return string;
	}
	/**
	 * 向后取list中n个元素,文本用#PCDATA代替
	 * */
	public static String getNElementReplace(List<String> sourceCodeList, int index, int n){
		String string = "";
		String element = "";
		for(int i = index; i < index + n; i++){
			if(i >= sourceCodeList.size()) return string;
			element = sourceCodeList.get(i);
			if(HtmlProcessUtils.isStr(element)) element = "#PCDATA";
				string += element;
		}
		return string;
	}
}
