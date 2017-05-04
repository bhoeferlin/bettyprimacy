package com.timerchina.pagetreematch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.timerchina.utils.TagArrayStack;

public class PrimaryTemplateGenerator {
	public static Set<String> wordSet = new HashSet<String>(); 
	static
	{
		wordSet.add("img");
		wordSet.add("input");
		wordSet.add("meta");
		wordSet.add("link");
	}
	
	//将源码中的字符串替换为#PCDATA
	private static void replace(List<String> list){
		for(int i = 0; i < list.size(); i ++){
			String ws = list.get(i);
			if(HtmlProcessUtils.isStr(ws)) {//字符串不匹配
				list.set(i, "#PCDATA");
			}
		}
	}
	//分割及模板合并
	private ArrayList<List<String>> segAndTemplateMerger(List<String> tw){
		replace(tw);
		ArrayList<List<String>> fragmentLists = new ArrayList<List<String>>();
		ArrayList<List<String>> fragmentListsTw = fragment(tw);
		List<String> template = new ArrayList<String>();
		for(int i = 0; i < fragmentListsTw.size(); i++){
			template = fragmentListsTw.get(i);
			PrimaryTemplateMerger.fragmentProcess(0, template.size(), template);
			fragmentLists.add(template);
		}
		return fragmentLists;
	}
	//初始模板构建
	public ArrayList<List<String>> buildPrimaryTemplate(String str){
		List<String> tw = HtmlProcessUtils.normalize(str);
		ArrayList<List<String>> fragmentListsTw = segAndTemplateMerger(tw);
		if(fragmentListsTw == null||fragmentListsTw.size() == 0){
			System.out.println("分段失败！");
			return null;
		}
		ArrayList<List<String>> fragmentLists = new ArrayList<List<String>>();
		int size = fragmentListsTw.size();
		List<String> template = new ArrayList<String>();
		for(int i = 1; i < size; i++){
			template = fragmentListsTw.get(i - 1);
			List<String> ts = HtmlProcessUtils.deepCopy(fragmentListsTw.get(i));
			List<String> fragmentList = new ArrayList<String>();
			fragmentList = buildPrimaryTemplate(template, ts);
			if(fragmentList == null||fragmentList.size() == 0) continue;
			fragmentLists.add(fragmentList);
		}
		return fragmentLists;
	}
	/**
	 * 生成初级模板
	 **/
	private List<String> buildPrimaryTemplate(List<String> tw, List<String> ts){
		int length = Math.max(tw.size(), ts.size());
		int delta = 1;
		for(int i = 0; i < length; i = i + delta){
			delta = 1;
			String ws = "";
			try {
				ws = tw.get(i);
			}catch (IndexOutOfBoundsException e) {//数据不平衡导致的越界错误，多余部分去除
				break;
			}
			try {
				String ss = ts.get(i);
				if(!ws.equals(ss)){//出现不匹配
					//括号不匹配：
					//1)将包含括号的项加到不包含括号的项上面;
					boolean isTemplate = false;
					if(ws.contains("(")||ss.contains("(")||ws.contains(")+")||ss.contains(")+")){
						isTemplate = isTemplateNotMatch(tw, ts, i);//判断是否为模板不匹配
						if (isTemplate) {//括号不匹配
							String wsTemp = ws.replace("(", "").replace(")+",
									"");
							String ssTemp = ss.replace("(", "").replace(")+",
									"");
							String sData = ws.length() > ss.length() ? ws : ss;
							if (wsTemp.equals(ssTemp)) {
								tw.set(i, sData);
							}
						}
						else { //2)模板与多余项的重叠
							delta = Cross.blockMatch(ts, tw, i);
							if(delta == -1) {
								delta = Cross.blockMatch(tw, ts, i);
								if(delta == -1){
									return null;
								}
							}
						}
					}
					else if(HtmlProcessUtils.isStr(ws)&&HtmlProcessUtils.isStr(ss)) {//字符串不匹配
						tw.set(i, "#PCDATA");
					}
					else {//标签不匹配，前退一项，采用十字交叉法匹配
						i = i - 1;
						delta = Cross.primaryCross(tw, ts, i);
						i = i + 1;
						if(delta == -1) {
							return null;
						}
					}
				}
				else {
					if(HtmlProcessUtils.isStr(ws))//字符串相等的文本
						tw.set(i, "#PCDATA");
				}
			} catch (IndexOutOfBoundsException e) {//数据不平衡导致的越界错误，多余部分去除
				HtmlProcessUtils.delListByIndex(tw, i, tw.size()-1);
				break;
			}
			length = Math.max(tw.size(), ts.size());
		}
		//删除连续的迭代符
		HtmlProcessUtils.delListByElement(tw,"placeholder");
		HtmlProcessUtils.delListRepeatElement(tw, "#PCDATA?");
		HtmlProcessUtils.delListRepeatElement(tw, "<S?>");
		return tw;
	}
	/**
	 * 判断标签不匹配是否为模板不匹配
	 * */
	private static boolean isTemplateNotMatch(List<String> tw, List<String> ts, int index){
		boolean isTemplate = false;
		String currentTw = tw.get(index);
		String currentTs = ts.get(index);
		String currentTwTemp = currentTw.replace("(", "").replace(")+", "");
		String currentTsTemp = currentTs.replace("(", "").replace(")+", "");
		int len = Math.max(tw.size(), ts.size());
		if(currentTwTemp.equals(currentTsTemp)){
			String twStr = "";
			String tsStr = "";
			for(int k = index; k < len; k++){
				try {
					currentTw = tw.get(k);
					currentTs = ts.get(k);
				} catch (Exception e) {
					break;
				}
				twStr += tw.get(k).replace("(", "").replace(")+", "");
				tsStr += ts.get(k).replace("(", "").replace(")+", "");
				if(currentTw.contains(")+")||currentTs.contains(")+")){
					if(twStr.equals(tsStr)){
						isTemplate = true;
						break;
					}
					else {
//						//模板前面包含多余项且多余项起始于模板起始相同
						break;
					}
				}
			}
		}
		return isTemplate;
	}
	/**
	 * 对输入的列表进行分段
	 * */
	public static ArrayList<List<String>> fragment(List<String> list){
		ArrayList<List<String>> fragmentLists = new ArrayList<List<String>>();
		List<String> tempList = new ArrayList<String>();
		TagArrayStack<String> tagStack = new TagArrayStack<String>();
		String stackData = "";
		for(int i = 0; i < list.size();i++){
			stackData = list.get(i); 
			String tag = HtmlProcessUtils.getTag(stackData);
			tempList.add(stackData);
			if(HtmlProcessUtils.isContainWord(tag, wordSet)||HtmlProcessUtils.isStr(stackData)){ //元素为正文或单标签，不入栈
				continue;
			} 
			else if(!stackData.contains("</")){
				tagStack.push(tag);
			}
			else {
				tagStack.pop();
				if(tagStack.isEmpty()) {
					fragmentLists.add(tempList);
					tempList = new ArrayList<String>();
				}
			}
		}
		return fragmentLists;
	}
}
