/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * 
 */
package org.wltea.analyzer.dic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.cfg.DefaultConfig;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {


	/*
	 * 词典单子实例
	 */
	private volatile static Dictionary singleton;
	
	/*
	 * 主词典对象
	 */
	private DictSegment _MainDict;
	
	/*
	 * 停止词词典 
	 */
	//private DictSegment _StopWordDict;
	/*
	 * 量词词典
	 */
	private DictSegment _QuantifierDict;
	/*
	 * 单字带词频词典
	 */
	private DictCharNode _CharFreqDict;
	/*
	 * 配置对象
	 */
	private Configuration cfg;
	
	private Dictionary(Configuration cfg){
		this.cfg = cfg;
		//建立一个主词典实例
		_MainDict = new DictSegment((char)0);
		this.loadMainDict(_MainDict);
		
		/*_StopWordDict = new DictSegment((char)0);
		this.loadStopWordDict(_StopWordDict);*/
		
		this.loadQuantifierDict();
		this.loadCharFreqDict();

	}
	
	/**
	 * 词典初始化
	 * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典，
	 * 这将延长首次分词操作的时间
	 * 该方法提供了一个在应用加载阶段就初始化字典的手段
	 * @return Dictionary
	 */
	public static Dictionary initial(Configuration cfg){
		if(singleton == null){
			synchronized(Dictionary.class){
				if(singleton == null){
					singleton = new Dictionary(cfg);
					return singleton;
				}
			}
		}
		return singleton;
	}
	
	/**
	 * 把solr配置的字典加入到MainDic中，进行字典切换
	 * @param inputStreamList 多字典输入流
	 * @return
	 */
	public static  synchronized Dictionary addDic2MainDic(List<InputStream> inputStreamList)
	{
		if(singleton == null)
		{
			Configuration cfg = DefaultConfig.getInstance();
			Dictionary.initial(cfg);
		}
		
		DictSegment mainDicTemp = new DictSegment((char)0);

		System.out.println("begin load MainDict :");
		singleton.loadMainDict(mainDicTemp);
		
		System.out.println("begin loadSolrMainDict by  List<InputStream>:");
		for(InputStream is : inputStreamList)
		{
			singleton.loadWords2DictSegment(is, mainDicTemp);
		}
		
		singleton._MainDict = mainDicTemp;
		System.out.println("*********************************");
		System.out.println("<mainWordsDic>end switch!!");
		System.out.println("*********************************");
		
		mainDicTemp = null;
	
		return singleton;
	}
	
	/**
	 * 获取词典单子实例
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getSingleton(){
		if(singleton == null){
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}
	
	/**
	 * 批量加载新词条
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量加载词条到主内存词典中
					singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 批量移除（屏蔽）词条
	 * @param words
	 */
	public void disableWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量屏蔽词条
					singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray , int begin, int length){
		return singleton._MainDict.match(charArray, begin, length);
	}
	
	/**
	 * 检索匹配量词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}
	
	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 * @param charArray
	 * @param currentIndex
	 * @param matchedHit
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}
	
	
	/**
	 * 判断是否是停止词
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	/*public boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWordDict.match(charArray, begin, length).isMatch();
	}*/	
	
	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict(DictSegment dstDicSegment){
		
		//读取主词典文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("main2012.dic");
        if(inputStream == null){
        	throw new RuntimeException("Main Dictionary not found!!!");
        }
        
        //System.out.println("test加载主字典");
        this.loadWords2DictSegment(inputStream,dstDicSegment);
        
        //System.out.println("test加载扩展字典");
    	this.loadExtDict(dstDicSegment);
       
	}	
	
	/**
	 * 加载用户配置的扩展词典到主词库表
	 */
	private void loadExtDict(DictSegment dstDicSegment){
		//加载扩展词典配置
		List<String> extDictFiles  = cfg.getExtDictionarys();
		if(extDictFiles != null){
			InputStream is = null;
			for(String extDictName : extDictFiles){
				//读取扩展词典文件
				//System.out.println("加载扩展词典：" + extDictName);
				is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
				//如果找不到扩展的字典，则忽略
				if(is == null){
					continue;
				}
				loadWords2DictSegment(is,dstDicSegment);
			}
		}		
	}
	
	/**
	 * 
	 * @param is 字典数据输入流
	 * @param dstDicSegment 目标字典
	 */

	private void loadWords2DictSegment(InputStream is,DictSegment dstDicSegment) {
		
		if(is != null)
		{
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is,
						"UTF-8"));
				String theWord = null;
				do {
					theWord = br.readLine();
					if (theWord != null ) {
						String line = theWord.trim();
						if (!line.isEmpty() && !line.startsWith("#")){
							String[] words = line.split("[\\s=,>]+");
							for(String w :words)
								dstDicSegment.fillSegment(w.toLowerCase().toCharArray());
						}
					}
				} while (theWord != null);

			} catch (IOException ioe) {
				System.err.println(" Dictionary loading exception。ClassName: " + dstDicSegment.getClass().getName());
				ioe.printStackTrace();
				
			} finally {
				try {
					if (is != null) {
						is.close();
						is = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
		
	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict(){
		//建立一个量词典实例
		_QuantifierDict = new DictSegment((char)0);
		//读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("quantifier.dic");
        if(is == null){
        	throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
		loadWords2DictSegment(is, _QuantifierDict);
	}
	
	private void loadCharFreqDict(){
		_CharFreqDict = new DictCharNode();
		//读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("chars.dic");
        if(is == null){
        	throw new RuntimeException("Chars Dictionary not found!!!");
        }
		try {		//此处可以抽象出一个接口，或公用函数
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					String[] w = theWord.split(" ");
					if(w.length == 2)
					{
						_CharFreqDict.addChar(w[0].charAt(0), (float)(Math.log10(Integer.parseInt(w[1])+5)));
					}
					/*else
					{
						_CharFreqDict.addChar(w[0].charAt(0), 0);		//默认无词性该给多少权重？是否该存在(对于没词频数据)？
					}*/
				}
			} while (theWord != null);
			
		} catch (IOException ioe) {
			System.err.println("Chars Dictionary loading exception.");
			ioe.printStackTrace();	
			}finally{
			try {
				if(is != null){
					is.close();
					is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public float getCharFreq(Character key)
	{
		return _CharFreqDict.getCharFreq(key);
	}
	
}
