package com.sxf;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.util.LuceneUtils;

public class LuceneTest {
	public static void main(String[] args) throws Exception{
		//准备索引数据
		List<Map<String, Object>> productNames = new ArrayList<Map<String, Object>>();
		List<String> contentList = new ArrayList<String>();
		contentList.add("飞利浦led灯泡e27螺口暖白球泡灯家用照明超亮节能灯泡转色温灯泡");
		contentList.add("飞利浦led灯泡e14螺口蜡烛灯泡3W尖泡拉尾节能灯泡暖黄光源Lamp");
		contentList.add("雷士照明 LED灯泡 e27大螺口节能灯3W球泡灯 Lamp led节能灯泡");
		contentList.add("飞利浦 led灯泡 e27螺口家用3w暖白球泡灯节能灯5W灯泡LED单灯7w");
		contentList.add("飞利浦led小球泡e14螺口4.5w透明款led节能灯泡照明光源lamp单灯");
		contentList.add("飞利浦蒲公英护眼台灯工作学习阅读节能灯具30508带光源");
		contentList.add("欧普照明led灯泡蜡烛节能灯泡e14螺口球泡灯超亮照明单灯光源");
		contentList.add("欧普照明led灯泡节能灯泡超亮光源e14e27螺旋螺口小球泡暖黄家用");
		contentList.add("聚欧普照明led灯泡节能灯泡e27螺口球泡家用led照明单灯超亮光源");	
		for (int i = 0; i < contentList.size(); i++) {
			Map<String, Object> datamap = new HashMap<String, Object>();
			datamap.put("id", i);
			datamap.put("content", contentList.get(i));
			productNames.add(datamap);
		}
		createIndex(productNames);
		// 准备查询器
		String keyword = "护眼带光源";
		Query query = new QueryParser("content", LuceneUtils.getAnalyzer()).parse(keyword);
		// 搜索
		IndexSearcher searcher = LuceneUtils.getIndexSearcher();
		int numberPerPage = 1000;
		System.out.printf("当前一共有%d条数据%n",productNames.size());
		System.out.printf("查询关键字是：\"%s\"%n",keyword);
		ScoreDoc[] hits = searcher.search(query, numberPerPage).scoreDocs;
		showSearchResults(searcher, hits, query);
	}
	
	private static void createIndex(List<Map<String, Object>> products) throws IOException {
		IndexWriter writer = LuceneUtils.getIndexWriter();
		for (Map<String, Object> datamap : products) {
			addDoc(writer, datamap);
		}
		writer.close();
	}
	
	private static void addDoc(IndexWriter w, Map<String, Object> datamap) throws IOException {
		Document doc = new Document();
		for(Map.Entry<String, Object> entry : datamap.entrySet()){
		    doc.add(new TextField(entry.getKey(), entry.getValue().toString(), Field.Store.YES));
			w.addDocument(doc);
		}
	}
	
	private static void showSearchResults(IndexSearcher searcher, ScoreDoc[] hits, Query query)
			throws Exception {
		System.out.println("找到 " + hits.length + " 个命中.");
		System.out.println("序号\t匹配度得分\t结果");
		for (int i = 0; i < hits.length; ++i) {
			ScoreDoc scoreDoc= hits[i];
			int docId = scoreDoc.doc;
			Document d = searcher.doc(docId);
			List<IndexableField> fields = d.getFields();
			System.out.print((i + 1));
			System.out.print("\t" + scoreDoc.score);
			for (IndexableField f : fields) {
				System.out.print("\t" + d.get(f.name()));
			}
			System.out.println();
		}
	}
}
