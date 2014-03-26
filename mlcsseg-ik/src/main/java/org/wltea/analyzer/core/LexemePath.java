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
 */
package org.wltea.analyzer.core;

import org.wltea.analyzer.dic.Dictionary;


/**
 * Lexeme链（路径）
 */
class LexemePath extends QuickSortSet implements Comparable<LexemePath>{
	
	//起始位置
	private int pathBegin;
	//结束
	private int pathEnd;
	//词元链的有效字符长度
	private int payloadLength;
	
	private char[] sentenceContent;	//原始输入内容
	private int absBegin;		//交集的绝对起始处----区别于词元
	private int absLength;		//交集的绝对长度
	
	private float _result=-1.0f;		//存储返回量化后的结果
	
	LexemePath(){
		this.pathBegin = -1;
		this.pathEnd = -1;
		this.payloadLength = 0;
	}

	LexemePath(char[] context,int absBegin ,int fullTextLength)
	{
		this.pathBegin = -1;
		this.pathEnd = -1;
		this.payloadLength = 0;
		//System.arraycopy(context, 0,sentenceContent, 0, 100);
		this.sentenceContent = context;
		this.absBegin = absBegin;
		this.absLength = fullTextLength;
	}
	/**
	 * 向LexemePath追加相交的Lexeme
	 * @param lexeme
	 * @return 
	 */
	boolean addCrossLexeme(Lexeme lexeme){
		if(this.isEmpty()){
			this.addLexeme(lexeme);
			this.pathBegin = lexeme.getBegin();
			this.pathEnd = lexeme.getBegin() + lexeme.getLength();
			this.payloadLength += lexeme.getLength();
			return true;
			
		}else if(this.checkCross(lexeme)){
			this.addLexeme(lexeme);
			if(lexeme.getBegin() + lexeme.getLength() > this.pathEnd){
				this.pathEnd = lexeme.getBegin() + lexeme.getLength();
			}
			this.payloadLength = this.pathEnd - this.pathBegin;	//此处payloadLength，交集处不算？end减原来的begin
			return true;
			
		}else{
			return  false;
			
		}
	}
	
	/**
	 * 向LexemePath追加不相交的Lexeme
	 * @param lexeme
	 * @return 
	 */
	boolean addNotCrossLexeme(Lexeme lexeme){
		if(this.isEmpty()){
			this.addLexeme(lexeme);
			this.pathBegin = lexeme.getBegin();
			this.pathEnd = lexeme.getBegin() + lexeme.getLength();
			this.payloadLength += lexeme.getLength();
			return true;
			
		}else if(this.checkCross(lexeme)){
			return  false;
			
		}else{
			this.addLexeme(lexeme);
			this.payloadLength += lexeme.getLength();
			Lexeme head = this.peekFirst();
			this.pathBegin = head.getBegin();
			Lexeme tail = this.peekLast();
			this.pathEnd = tail.getBegin() + tail.getLength();
			return true;
			
		}
	}
	
	/**
	 * 移除尾部的Lexeme
	 * @return
	 */
	Lexeme removeTail(){
		Lexeme tail = this.pollLast();
		if(this.isEmpty()){
			this.pathBegin = -1;
			this.pathEnd = -1;
			this.payloadLength = 0;			
		}else{		
			this.payloadLength -= tail.getLength();
			Lexeme newTail = this.peekLast();
			this.pathEnd = newTail.getBegin() + newTail.getLength();
		}
		return tail;
	}
	
	/**
	 * 检测词元位置交叉（有歧义的切分）
	 * @param lexeme
	 * @return
	 */
	boolean checkCross(Lexeme lexeme){
		return (lexeme.getBegin() >= this.pathBegin && lexeme.getBegin() < this.pathEnd)
				|| (this.pathBegin >= lexeme.getBegin() && this.pathBegin < lexeme.getBegin()+ lexeme.getLength());
	}
	
	int getPathBegin() {
		return pathBegin;
	}

	int getPathEnd() {
		return pathEnd;
	}

	/**
	 * 获取Path的有效词长
	 * @return
	 */
	int getPayloadLength(){
		return this.payloadLength;
	}
	
	/**
	 * 获取LexemePath的路径长度
	 * @return
	 */
	int getPathLength(){
		return this.pathEnd - this.pathBegin;
	}
	

	/**
	 * X权重（词元长度积），长度越平均，值越大
	 * @return
	 */
	int getXWeight(){
		int product = 1;
		Cell c = this.getHead();
		while( c != null && c.getLexeme() != null){
			product *= c.getLexeme().getLength();
			c = c.getNext();
		}
		return product;
	}
	
	/**
	 * 词元位置权重,切分结果词元越多，值为大
	 * @return
	 */
	int getPWeight(){
		int pWeight = 0;
		int p = 0;
		Cell c = this.getHead();
		while( c != null && c.getLexeme() != null){
			p++;
			//pWeight += c.getLexeme().getBegin() * c.getLexeme().getLength();
			pWeight += p * c.getLexeme().getLength();
			c = c.getNext();
		}
		return pWeight;		
	}
	
	LexemePath copy(){
		LexemePath theCopy = new LexemePath();
		theCopy.pathBegin = this.pathBegin;
		theCopy.pathEnd = this.pathEnd;
		theCopy.payloadLength = this.payloadLength;
		
		theCopy.sentenceContent = this.sentenceContent;
		theCopy.absBegin = this.absBegin;
		theCopy.absLength = this.absLength;
		
		Cell c = this.getHead();
		while( c != null && c.getLexeme() != null){
			theCopy.addLexeme(c.getLexeme());
			c = c.getNext();
		}
		return theCopy;
	}

	public int compareTo(LexemePath o) {
		float nowResult,OriginResult;
		nowResult = this.calcResult();
		OriginResult = o.calcResult();
		
		if( nowResult > OriginResult )
		{
			return 1;
		}
		else if(nowResult < OriginResult)
		{
			return -1;
		}
		else 
		{
			if(this.pathEnd > o.pathEnd)
			{
				return 1;
			}
			else if(pathEnd < o.pathEnd)
			{
				return -1;
			}
		}
		return 0;
	}
	
	private float calcResult(){
		if(_result == -1.0f)		//未被计算过
		{
			_result= (this.payloadLength*10) + (this.size()*(-5)) + this.getPathLength()+this.getXWeight()+this.getPWeight();
			
			/*存在单字
			 *①、判断单字的个数，进行单字定位，用于获取
			 *②、在单字字典进行查找，是否存在，取其概率值 
			 * */
			if(this.payloadLength < this.absLength)		//存在单字
			{
				int curPoint;
				Cell head = this.getHead();
				curPoint = this.absBegin;		//从路径绝对起始处开始扫描
				float sumFreq=0;
				char singleChar=0;
				while(head != null){				
					while(curPoint<head.getLexeme().getBegin())
					{
						singleChar=sentenceContent[curPoint];		//会空指针？自定义copy函数原因
						sumFreq += Dictionary.getSingleton().getCharFreq(singleChar);
						//result -=;
						curPoint++;
					}
								
					curPoint += head.getLexeme().getLength();	//平移一个词元的长度
					head = head.getNext();
				}
				
				//词元扫描完，结尾可能漏词
				while(curPoint < this.absBegin + this.absLength)
				{
					singleChar=sentenceContent[curPoint];		//会空指针？自定义copy函数原因
					sumFreq += Dictionary.getSingleton().getCharFreq(singleChar);
					curPoint++;
				}
				
				_result += sumFreq*2.0;	//存在单字出现频率越高，则路径越优异
			}
		}
		return _result;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("pathBegin  : ").append(pathBegin).append("\r\n");
		sb.append("pathEnd  : ").append(pathEnd).append("\r\n");
		sb.append("payloadLength  : ").append(payloadLength).append("\r\n");
		Cell head = this.getHead();
		while(head != null){
			sb.append("lexeme : ").append(head.getLexeme()).append("\r\n");
			head = head.getNext();
		}
		return sb.toString();
	}

}
