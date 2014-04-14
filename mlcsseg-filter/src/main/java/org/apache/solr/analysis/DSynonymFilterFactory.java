package org.apache.solr.analysis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.Version;

import com.mlcs.search.mlcsseg.common.ScheduledExecutor;


public class DSynonymFilterFactory extends TokenFilterFactory implements
ResourceLoaderAware {

	public DSynonymFilterFactory(Map<String, String> args) throws IOException {
		super(args);
		expand = getBoolean(args, "expand", true);
		ignoreCase = getBoolean(args, "ignoreCase", false);
		conf = get(args, "conf"); //paths & lastupdate
		System.out.println(conf);
	}

	private SynonymMap map; // 词库，可以通过引用改变
	private boolean ignoreCase; //属性
	private boolean expand; 
	private ResourceLoader loader = null;

	private String conf;    // properties格式， 存lastupdatetime和词库路径files：逗号间隔
	private long lastUpdateTime = -1;

	public void inform(ResourceLoader loader) throws IOException {
		System.out.println(":::::synonym::::::::::::::::::::::" + conf);
		this.loader = loader;
		this.update();
		if(conf != null && !conf.trim().isEmpty()){
			ScheduledExecutor.submit(new Runnable() {
				
				public void run() {
					update();
					
				}
			}, 1000 * 60);
		}
	}

	private SynonymMap loadSolrSynonyms(ResourceLoader loader, Properties p) throws IOException, ParseException {
		final Analyzer analyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				WhitespaceTokenizer tokenizer =  new WhitespaceTokenizer(Version.LUCENE_44, reader);
				TokenStream stream = ignoreCase ? new LowerCaseFilter(Version.LUCENE_44, tokenizer) : tokenizer;
				return new TokenStreamComponents(tokenizer, stream);
			}
		};
		String synonyms = p.getProperty("files");

		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		SolrSynonymParser parser = new SolrSynonymParser(true, expand,	analyzer);
		
		File synonymFile = new File(synonyms);
		if (loader != null){ //first call in constructor
			if (synonymFile.exists()) {
				decoder.reset();
				parser.add(new InputStreamReader(loader.openResource(synonyms)));
			} else {
				List<String> files = splitFileNames(synonyms);
				for (String file : files) {
					decoder.reset();
					parser.add(new InputStreamReader(loader.openResource(file)));
				}
			}
		}

		return parser.build();
	}

	@Override
	public TokenStream create(TokenStream input) {
		return map.fst == null ? input : new SynonymFilter(input, map,ignoreCase);
	}

	public void update() {

		Properties p = canUpdate();
		if (p != null){
			try {
				System.out.println("<IKSynonymFilterFactory> updating !");
				map = loadSolrSynonyms(loader, p); // 内部已实现切换
				System.out.println("<IKSynonymFilterFactory> finish~!");
			} catch (IOException e) {
				System.err.println("<IKSynonymFilterFactory> IOException!!");
				e.printStackTrace();
			} catch (ParseException e) {
				System.err.println("<IKSynonymFilterFactory> ParseException!!");
				e.printStackTrace();
			}
		}
	}

	private Properties canUpdate() {

		try{
			Properties p = new Properties();
			InputStream confStream = loader.openResource(conf);
			p.load(confStream);
			confStream.close();
			String lastupdate = p.getProperty("lastupdate", "0");
			Long t = new Long(lastupdate);
			
			if (t > this.lastUpdateTime){
				this.lastUpdateTime = t.longValue();
				String paths = p.getProperty("files");
				if (paths==null || paths.trim().isEmpty()) // 必须有地址
					return null;
				System.out.println("loading conf");
				return p;
			}else{
				this.lastUpdateTime = t.longValue();
				return null;
			}
		}catch(Exception e){
			System.err.println("synonym parsing conf NullPointerException~~~~~" + e.getMessage());
			return null;
		}
	}

}
