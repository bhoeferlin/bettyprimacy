package com.timerchina.pagetreematch.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.timerchina.pagetreematch.HtmlProcessUtils;
import com.timerchina.pagetreematch.PrimaryTemplateGenerator;

public class PTMParser {
	private static int fieldCount = 1;
	
	public static List<Map<String, String>> process(List<String> regexList, List<String> extractorList){
		List<Map<String, String>> extractResultList = new ArrayList<Map<String, String>>(); 
		//对源码进行分段处理
		ArrayList<List<String>> sourceCodeLists = PrimaryTemplateGenerator.fragment(extractorList);
		for(int i = 0; i < sourceCodeLists.size(); i++){
			List<String> sourceCodeList = sourceCodeLists.get(i);
			Map<String, String> resultMap = parser(regexList, sourceCodeList);
			extractResultList.add(resultMap);
		}
		return extractResultList;
	}
	/**
	 * 模板解析过程
	 * */
	private static Map<String, String> parser(List<String> regexListSource, List<String> sourceCodeList){
		List<String> regexList = HtmlProcessUtils.deepCopy(regexListSource);
		Map<String, String> resultMap = new HashMap<String, String>();
		int delta = 1;
		fieldCount = 1;
		for(int i = 0; i < sourceCodeList.size(); i = i + delta){
			delta = 1;
			String sourceCode = "";
			String regexTag = "";
			try {
				sourceCode = sourceCodeList.get(i);
				regexTag = regexList.get(i);
			} catch (Exception e) {
				System.out.println("越界");
			}
			if(!sourceCode.equals(regexTag)){
				if(regexTag.equals("#PCDATA")){//直接提取
					resultMap.put(fieldCount+"", sourceCode);
					fieldCount ++;
				}
				else if(regexTag.contains("?")) {//可选项
					delta = optionParser(regexList, sourceCodeList, i);
					if(delta > 0 ){//有可选项时
						if(regexTag.contains("#PCDATA")){
							String cStr = "";
							for(int j = 0; j < delta; j++){
								cStr += sourceCodeList.get(i + j);
							}
							//添加序号
							resultMap.put(fieldCount+"", cStr);
							fieldCount ++;
						}
						continue;
					}
					else if(delta == 0) {
						delta = 1;
						continue;
					}
					delta = 1;
					//添加空项及序号
					if(regexTag.contains("#PCDATA?")){
						resultMap.put(fieldCount+"", "");
						fieldCount ++;
					}
				}
				else if(regexTag.contains("(")){//包含压缩
					delta = iteratorParser(regexList, sourceCodeList, i, resultMap);
				}
			}
		}
		return resultMap;
	}
	//处理压缩
	private static int iteratorParser(List<String> regexList, List<String> sourceCodeList, int index, Map<String, String> resultMap){
		int delta = 0;
		int nestLocate = index;
		int nestCount = 0;
		boolean isNest = false;
		String regexTag = regexList.get(index).replace("(", "");
		//获取模板
		int templateLen = 1;
		String templateStr = regexTag;
		for(int j = index + 1; j < regexList.size(); j++){
			regexTag = regexList.get(j);
			if(regexTag.contains("(")&&!regexTag.contains("?")){//包含迭代嵌套
				//记录下位置，回退迭代
				isNest = true;
				nestCount ++;
				nestLocate = j - index;
			}
			else if(regexTag.contains("(")&&regexTag.contains("?")){//迭代中包含可选项
				//匹配可选项
				int c = optionParser(regexList, sourceCodeList, j);
				if(c > 0 ){//有可选项时
					if(regexTag.contains("#PCDATA")){
						String cStr = "";
						for(int k = 0; k < c; k++){
							cStr += sourceCodeList.get(j + k);
						}
						//添加序号
						resultMap.put(fieldCount+"", cStr);
						fieldCount ++;
					}
					continue;
				}
				//添加空项及序号
				if(regexTag.contains("#PCDATA?")){
					resultMap.put(fieldCount+"", "");
					fieldCount ++;
				}
			}
			if(regexTag.contains("(")&&!regexTag.contains("?")) regexTag = regexTag.replace("(", "");
			if(regexTag.contains(")+")){
				nestCount --;
				regexTag = regexList.get(j).replace(")+", "");
				if(nestCount < 0){
					templateLen ++;
					templateStr += regexTag;
					break;
				}
			}
			templateLen ++;
			templateStr += regexTag;
		}
		//获取源码初始模板数据
		String sourceCodeStr = "";
		String strTemp = "";
//		String strTempPre = strTemp;
		for(int i = index; i < index + templateLen; i++){
			strTemp = sourceCodeList.get(i);
//			if(strTemp.equals("placeholder")){
//				if(strTempPre.equals("(S)?")) continue;
//				strTemp = "(S)?";
//			}
			if(HtmlProcessUtils.isStr(strTemp)){
//				extractResultList.add(strTemp);
				resultMap.put(fieldCount+"", strTemp);
				fieldCount ++;
				strTemp = "#PCDATA";
			} 
			sourceCodeStr += strTemp;
//			strTempPre = strTemp;
		}
		if(!templateStr.equals(sourceCodeStr)) 
			return 1;
		int j = index + templateLen;
		//遍历源码，获取所有模板对应数据
		while(templateStr.equals(sourceCodeStr)){
			delta = delta + templateLen;
			sourceCodeStr = "";
			//向后推，取模板长度的字符串，如果不相等，则跳出循环
			String sStr = "";
			if(j + templateLen <= sourceCodeList.size())
				sStr = HtmlProcessUtils.getNElementReplace(sourceCodeList, j, templateLen);
			if(!templateStr.equals(sStr)) break;
			
			for(int i = j; i < j + templateLen; i++){
				strTemp = sourceCodeList.get(i);
				if(HtmlProcessUtils.isStr(strTemp)){
					resultMap.put(fieldCount+"", strTemp);
					fieldCount ++;
					strTemp = "#PCDATA";
				} 
				sourceCodeStr += strTemp;
			}
			j = j + templateLen;
		}
		if(isNest){//如果包含嵌套，需回退到嵌套处，进行下一次处理
			return nestLocate;
		}
		//填充
		for(int c = index; c < index + delta - templateLen; c++)
			regexList.add(c, "placeholder");
		return delta;
	}
	//处理可选
	private static int optionParser(List<String> regexList, List<String> sourceCodeList, int index){
		int delta = 1;
		String sStr = "";
		String rStr = "";
		String sourceCodeTag = sourceCodeList.get(index);
		String regexTag1 = regexList.get(index + 1).replace("(", "").replace(")", "");
		
		//判断正则后三项是否包含可选，不包含时，可用后三项作为相等的判断
		boolean flag = false;
		for(int i = index + 1; i < index + 4; i++){
			if(i < regexList.size()){
				if(regexList.get(i).contains("?")) flag = true;
			}
		}
		if(!flag){
			try {
				sStr = HtmlProcessUtils.getNElementReplace(sourceCodeList, index, 3);
				rStr = HtmlProcessUtils.getNElement(regexList, index + 1, 3);
			} catch (Exception e1) {
//				System.out.println("后三项越界");
				sStr = "";
				rStr = "";
			}
		}
		if (regexTag1.equals(sourceCodeTag)&&sStr.equals(rStr)) {  // 无可选项时 
			//可选且下一个元素为模板时，判断解析元素是否与模板元素相等，相等则跳过该可选
			String rNextStr = regexList.get(index + 1);
			sourceCodeList.add(index, "placeholder");
			if(rNextStr.contains("(")){
				return 0;
			}
			return -1;
		}
		for (int k = index + 1; k < sourceCodeList.size(); k++) {  // 有可选项时
			sourceCodeTag = sourceCodeList.get(k);
			if(!flag){
				try {
					sStr = HtmlProcessUtils.getNElementReplace(sourceCodeList, k, 3);
				} catch (Exception e) {
					sStr = "";
					rStr = "";
				}
			}
			if (regexTag1.equals(sourceCodeTag) && sStr.equals(rStr)) {
				break;
			}
			delta++;
		}
		for (int c = index + 1; c < index + delta; c++)
			regexList.add(index, "placeholder");
		return delta;
	}
}
