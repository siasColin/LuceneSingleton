package com.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

//工具类，获取Lucene操作对象
public class LuceneUtils {
   public static final String INDEX_DIR="E:\\eclipse4.4workspace\\LuceneIndex";//索引文件存放目录
   private static Directory directory;//索引文件存放目录对象
   private static IndexWriter indexWriter;//索引写对象,线程安全
   private static IndexReader indexReader;//索引读对象，线程安全
   private static IndexSearcher indexSearcher;//索引搜索对象，线程安全
   private static Analyzer analyzer;//分词器对象

   static{
      try {
         //初始化索引文件存放目录对象
         directory= FSDirectory.open(Paths.get(INDEX_DIR));


         // 虚拟机退出时关闭
         Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
               System.out.println("--------释放关闭资源中....");
               try{
                  //释放关闭资源
                  if(null!=indexWriter){
                     indexWriter.close();
                  }
                  if(null!=indexReader){
                     indexReader.close();
                  }
                  if(null!=directory){
                     directory.close();
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               System.out.println("--------释放关闭资源成功....");
            }
         });
         
      } catch (Exception e) {
         e.printStackTrace();
      }
   }



   /**
    * 获取IndexWriter
    * @return
    */
   public static IndexWriter getIndexWriter() throws IOException {
      if(null==indexWriter){
         //初始化IK分词器
         Analyzer analyzer = getAnalyzer();
         //初始化索引的写配置对象
         IndexWriterConfig indexWriterConfig=new IndexWriterConfig(analyzer);
         indexWriterConfig.setRAMBufferSizeMB(48);
         indexWriterConfig.setOpenMode(OpenMode.CREATE);
         //初始化索引的写对象
         indexWriter=new IndexWriter(directory, indexWriterConfig);
      }
      return indexWriter;
   }
   /**
    * 获取IndexReader，可重用一些旧的 IndexReader
    * @return
    * @throws Exception 
    */
   public static IndexReader getIndexReader() throws IOException {
      if(indexReader==null) {
         indexReader = DirectoryReader.open(directory);
        } else {
            // 如果 IndexReader 不为空，就使用 DirectoryReader 打开一个索引变更过的 IndexReader 类
           //如果改变会返回一个新的reader，没有改变返回null
            IndexReader newIndexReader = DirectoryReader.openIfChanged((DirectoryReader)indexReader);
            if(newIndexReader!=null) {
               //此时要记得把旧的索引Reader对象关闭
               indexReader.close();
                indexReader = newIndexReader;
            }
        }
      return indexReader;
   }
   
   /**
    * 获取IndexSearcher
    * @return
    * @throws Exception
    */
   public static IndexSearcher getIndexSearcher() throws IOException {
      if(null==indexSearcher){
         indexSearcher=new IndexSearcher(getIndexReader());
      }
      return indexSearcher;
   }

   /**
    *     获取分词器对象
    * @return
    */
   public static Analyzer getAnalyzer(){
      if(null==analyzer){
         //分词器对象
         analyzer=new IKAnalyzer();;
      }
      return analyzer;
   }

   /**
    * 打印一个文档的所有字段的内容
    * @param
    */
   public static void printDocument(Document document){
      //打印具体字段
      List<IndexableField> fieldList = document.getFields();
      //遍历列表
      for (IndexableField field : fieldList){
         //打印出所有的字段的名字和值（必须存储了的）
         System.out.println(field.name()+":"+field.stringValue());
      }
      //文档详情
      System.out.println(document.toString());
   }

   /**
    * 打印ScoreDoc
    * @param scoreDoc
    * @throws IOException
    */
   public static void printScoreDoc(ScoreDoc scoreDoc) throws IOException{
      //获取文档的编号（类似索引主键）
      int docId = scoreDoc.doc;
      System.out.println("======文档编号："+docId);
      // 取出文档得分
      System.out.println("得分： " + scoreDoc.score);
      //获取具体文档
      Document document = indexSearcher.doc(docId);
      //打印具体字段
      printDocument(document);
   }

   /**
    * 打印命中的文档（带得分）的详情
    * @param topDocs
    */
   public static void printTopDocs(TopDocs topDocs) throws IOException {
      //1)打印总记录数（命中数）：类似于百度为您找到相关结果约100,000,000个
      long totalHits = topDocs.totalHits;
      System.out.println("查询（命中）总的文档条数："+totalHits);
      System.out.println("查询（命中）文档最大分数："+topDocs.getMaxScore());
      //2)获取指定的最大条数的、命中的查询结果的文档对象集合
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;
      //打印具体文档
      for (ScoreDoc scoreDoc : scoreDocs) {
         printScoreDoc(scoreDoc);
      }
   }

   public static void printTopDocsByQueryForHighlighter(Query query, int n) throws Exception{

      //=========1.创建一个高亮工具对象
      // 格式化器：参数1：前置标签，参数2：后置标签
      Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
      //打分对象，参数：query里面的条件，条件里面有搜索关键词
      Scorer fragmentScorer = new QueryScorer(query);
      //高亮工具
      //参数1.需要高亮什么颜色, 参数2.将哪些关键词进行高亮
      Highlighter highlighter = new Highlighter(formatter, fragmentScorer);
      //=======搜索相关
      IndexSearcher indexSearcher = getIndexSearcher();
      // 搜索数据,两个参数：查询条件对象要查询的最大结果条数
      // 返回的结果是 按照匹配度排名得分前N名的文档信息（包含查询到的总条数信息、所有符合条件的文档的编号信息）
      TopDocs topDocs = indexSearcher.search(query, n);
      // 打印命中的总条数
      System.out.println("本次搜索共" + topDocs.totalHits + "条数据,最高分："+topDocs.getMaxScore());

      // 获取得分文档对象（ScoreDoc）数组.SocreDoc中包含：文档的编号、文档的得分
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;

      //循环
      for (ScoreDoc scoreDoc : scoreDocs) {
         // 取出文档编号
         int docID = scoreDoc.doc;
         System.out.println("=========文档的编号是："+docID);
         // 取出文档得分
         System.out.println("当前文档得分： " + scoreDoc.score);
         // 根据编号去找文档
         Document document = indexSearcher.doc(docID);
         //获取文档的所有字段对象
         List<IndexableField> fieldList= document.getFields();
         //遍历列表
         for (IndexableField field : fieldList) {
            String highlighterValue = highlighter.getBestFragment(getAnalyzer(), field.name(), field.stringValue());
            //如果没有得到高亮的值
            if (null==highlighterValue) {
               //则让高亮结果等不高亮的值
               highlighterValue = field.stringValue();
            }
            //打印出所有的字段的名字和值（必须存储了的）
            System.out.println(field.name()+":"+highlighterValue);
         }

      }
   }


}
