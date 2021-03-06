package com.timerchina.utils;

public interface TagStack<T> {
	 /** 
     * 判断栈是否为空 
     */  
    boolean isEmpty();  
    /** 
     * 清空栈 
     */  
    void clear();  
    /** 
     * 栈的长度 
     */  
    int length();  
    /** 
     * 数据入栈 
     */  
    boolean push(T data);  
    /** 
     * 数据出栈 
     */  
    T pop();  
    /**
     * 栈顶元素
     */
    T getElement();
}
