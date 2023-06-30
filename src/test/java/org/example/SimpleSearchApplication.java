package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.Term;

import java.util.ArrayList;
import java.util.List;

public class SimpleSearchApplication {
    private Directory memoryIndex;
    private StandardAnalyzer analyzer;

    public SimpleSearchApplication() {
        this(new ByteBuffersDirectory(), new StandardAnalyzer());
    }

    public SimpleSearchApplication(ByteBuffersDirectory memoryIndex, StandardAnalyzer analyzer) {
        this.memoryIndex = memoryIndex;
        this.analyzer = analyzer;
    }

    public void createIndex(String title, String body) {
        try (IndexWriter writer = new IndexWriter(memoryIndex, new IndexWriterConfig(analyzer))) {
            Document document = new Document();
            document.add(new TextField("title", title, TextField.Store.YES));
            document.add(new TextField("body", body, TextField.Store.YES));
            writer.addDocument(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Document> searchIndex(String inField, String queryString) {
        try {
            Query query = new QueryParser(inField, analyzer).parse(queryString);
            return searchIndex(query);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Document> searchIndex(Query query) {
        try {
            IndexReader indexReader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 10);
            List<Document> documents = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                documents.add(searcher.doc(scoreDoc.doc));
            }
            return documents;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteDocuments(Term term) {
        try (IndexWriter writer = new IndexWriter(memoryIndex, new IndexWriterConfig(analyzer))) {
            writer.deleteDocuments(term);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SimpleSearchApplication searchApp = new SimpleSearchApplication();

        // Creating and adding documents to the index
        searchApp.createIndex("Document 1", "This is the body of document 1.");
        searchApp.createIndex("Document 2", "This is the body of document 2.");

        // Searching the index
        List<Document> searchResults = searchApp.searchIndex("body", "body");

        // Printing the search results
        for (Document document : searchResults) {
            System.out.println("Title: " + document.get("title"));
            System.out.println("Body: " + document.get("body"));
            System.out.println();
        }

        // Deleting documents based on a term
        Term term = new Term("title", "Document 2");
        searchApp.deleteDocuments(term);

        // Searching the index after deletion
        List<Document> updatedSearchResults = searchApp.searchIndex("body", "body");

        // Printing the updated search results
        System.out.println("After deletion:");
        for (Document document : updatedSearchResults) {
            System.out.println("Title: " + document.get("title"));
            System.out.println("Body: " + document.get("body"));
            System.out.println();
        }
    }

    public void deleteDocument(Term term) {
    }
}
