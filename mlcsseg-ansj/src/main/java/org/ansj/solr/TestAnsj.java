package org.ansj.solr;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;



public class TestAnsj {
	
	public static void main(String[] args) throws IOException {
		List<Term> parse = ToAnalysis.parse("天天向上，媒体打打。《回家真好》");
		System.out.println(parse);
		Tokenizer tokenizer = new AnsjTokenizer(new StringReader("天天向上，媒体打打。《回家真好》"), 0, true);
		CharTermAttribute termAtt = tokenizer.addAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = 
				tokenizer.addAttribute(OffsetAttribute.class);
			PositionIncrementAttribute positionIncrementAtt = 
				tokenizer.addAttribute(PositionIncrementAttribute.class);

		
		while (tokenizer.incrementToken()){

			System.out.print(new String(termAtt.toString()) );
			System.out.print( offsetAtt.startOffset() + "-" + offsetAtt.endOffset() + "-" );
			System.out.print( positionIncrementAtt.getPositionIncrement() +"/");

		}
		tokenizer.close();
	}
}
