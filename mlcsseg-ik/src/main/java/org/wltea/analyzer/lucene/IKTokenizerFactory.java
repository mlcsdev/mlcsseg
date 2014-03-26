package org.wltea.analyzer.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.util.AttributeSource.AttributeFactory;
import org.wltea.analyzer.dic.Dictionary;

import com.mlcs.search.mlcsseg.lucene.ReloadableTokenizerFactory;
import com.mlcs.search.mlcsseg.lucene.ReloaderRegister;

public class IKTokenizerFactory extends ReloadableTokenizerFactory {


	public IKTokenizerFactory(Map<String, String> args) {
		super(args);
		
		useSmart = getBoolean(args, "useSmart", false);
		System.out.println(":::ik:construction::::::::::::::::::::::::::" + conf);
	}
	private boolean useSmart = false;

	private boolean useSmart() {
		return useSmart;
	}


	// 通过这个实现，调用自身分词器
	public Tokenizer create(AttributeFactory attributeFactory, Reader in) { // 会多次被调用
		return new IKTokenizer(in, this.useSmart()); // 初始化词典，分词器，消歧器
	}

	public void inform(ResourceLoader loader) throws IOException { // 在启动时初始化一次
		System.out.println(":::ik:::inform::::::::::::::::::::::::" + conf);
		ReloaderRegister.register(this, loader, conf);
	}



	@Override
	public void update(List<InputStream> inputStreams) {
		Dictionary.addDic2MainDic(inputStreams);
	}


}
