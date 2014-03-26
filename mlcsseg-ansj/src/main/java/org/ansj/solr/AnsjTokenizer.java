package org.ansj.solr;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import com.mlcs.search.mlcsseg.lucene.CnTokenizer;


public class AnsjTokenizer extends CnTokenizer{
	private int analysisType ; 
	private boolean removePunc;

	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAtt;
	private TypeAttribute typeAtt;
	int lastOffset = 0;
	int endPosition =0; 
	private Iterator<Term> tokenIter;
	private List<Term> tokenBuffer;
	static
	{
		ToAnalysis.parse("");
	}
	
	public AnsjTokenizer(Reader input, int analysisType, boolean removePunc) {
		super(input);
		offsetAtt = addAttribute(OffsetAttribute.class);
		termAtt = addAttribute(CharTermAttribute.class);
		typeAtt = addAttribute(TypeAttribute.class);
		this.analysisType = analysisType;
		this.removePunc = removePunc;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (tokenIter == null || !tokenIter.hasNext()){
			String currentSentence = checkSentences();
			if (currentSentence!= null){
				tokenBuffer = new ArrayList<Term>();
				if (analysisType == 1){
					for(Term term :  ToAnalysis.parse(currentSentence)){
						if (removePunc && stopwords.contains(term.getName()))
							continue;
						tokenBuffer.add(term);
					}
					
				}else {
					for(Term term :  IndexAnalysis.parse(currentSentence)){
						if (removePunc && stopwords.contains(term.getName()))
							continue;
						tokenBuffer.add(term);
					}
				}
				tokenIter = tokenBuffer.iterator();
				if (!tokenIter.hasNext()){
					return false;
				}
			} else {
				return false; // no more sentences, end of stream!
			}
		}
		clearAttributes();
		
		Term term = tokenIter.next();
		if (removePunc){
			while(stopwords.contains(term.getName())){
				if (!tokenIter.hasNext()){
				}else{
					term = tokenIter.next();
				}
			}
		}
		termAtt.append(term.getName());
		termAtt.setLength(term.getName().length());
		
		int currentStart = tokenStart + term.getOffe();
		int currentEnd = tokenStart + term.getToValue();
		offsetAtt.setOffset(currentStart,currentEnd);
		typeAtt.setType("word");

//		int pi = currentStart - lastOffset;
//		if(term.getOffe()  <= 0) {
//			pi = 1;
//		}
//		positionIncrementAtt.setPositionIncrement( pi );
		lastOffset = currentStart;
		endPosition = currentEnd;
		return true;
	}



	@Override
	public void reset() throws IOException {
		super.reset();
	}
	
	public final void end() {
		// set final offset
		int finalOffset = correctOffset(this.endPosition);
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

}
