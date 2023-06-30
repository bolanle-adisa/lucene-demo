package org.example;


import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.Test;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import static org.junit.Assert.assertEquals;

public class SimpleSearchApplicationTest {

    @Test
    public void givenSearchQueryWhenFetchedDocumentThenCorrect() throws ParseException {
        SimpleSearchApplication searchApp = new SimpleSearchApplication();

        searchApp.createIndex("Hello world", "Some hello world");

        List<Document> documents = searchApp.searchIndex("body", "world");

        assertEquals("Hello world", documents.get(0).get("title"));
    }

    @Test
    public void givenTermQueryWhenFetchedDocumentThenCorrect() throws ParseException {
        SimpleSearchApplication simpleSearchApplication = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("activity", "running in track");
        simpleSearchApplication.createIndex("activity", "Cars are running on road");

        List<Document> documents = simpleSearchApplication.searchIndex("body", "running");
        assertEquals(2, documents.size());
    }

    @Test
    public void givenPrefixQueryWhenFetchedDocumentThenCorrect() {
        SimpleSearchApplication simpleSearchApplication
                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("article", "Lucene introduction");
        simpleSearchApplication.createIndex("article", "Introduction to Lucene");

        Term term = new Term("body", "intro");
        Query query = new PrefixQuery(term);

        List<Document> documents = simpleSearchApplication.searchIndex(query);
        assertEquals(2, documents.size());
    }

    @Test
    public void givenWildcardQueryWhenFetchedDocumentThenCorrect() {
        SimpleSearchApplication simpleSearchApplication
                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("article", "Lucene introduction");
        simpleSearchApplication.createIndex("article", "Introduction to Lucene");

        Term term = new Term("body", "intro*");
        Query query = new WildcardQuery(term);

        List<Document> documents = simpleSearchApplication.searchIndex(query);
        assertEquals(2, documents.size());
    }

    @Test
    public void givenPhraseQueryWhenFetchedDocumentThenCorrect() {
        SimpleSearchApplication simpleSearchApplication
                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("quotes", "A rose by any other name would smell as sweet.");

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.add(new Term("body", "smell"));
        builder.add(new Term("body", "sweet"));
        builder.setSlop(1);  // Distance in the number of words between the terms
        Query query = builder.build();

        List<Document> documents = simpleSearchApplication.searchIndex(query);
        assertEquals(1, documents.size());
    }

    @Test
    public void givenFuzzyQueryWhenFetchedDocumentThenCorrect() {
        SimpleSearchApplication simpleSearchApplication
                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("article", "Halloween Festival");
        simpleSearchApplication.createIndex("decoration", "Decorations for Halloween");

        Term term = new Term("body", "hallowen");  // Intentionally misspelled
        Query query = new FuzzyQuery(term);

        List<Document> documents = simpleSearchApplication.searchIndex(query);
        assertEquals(2, documents.size());
    }

    @Test
    public void givenBooleanQueryWhenFetchedDocumentThenCorrect() {
        SimpleSearchApplication simpleSearchApplication
                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("Destination", "Las Vegas singapore car");
        simpleSearchApplication.createIndex("Commutes in singapore", "Bus Car Bikes");

        Term term1 = new Term("body", "singapore");
        Term term2 = new Term("body", "car");

        Query query1 = new TermQuery(term1);
        Query query2 = new TermQuery(term2);

        BooleanQuery booleanQuery
                = new BooleanQuery.Builder()
                .add(query1, BooleanClause.Occur.MUST)
                .add(query2, BooleanClause.Occur.MUST)
                .build();

        List<Document> documents = simpleSearchApplication.searchIndex(booleanQuery);
        assertEquals(1, documents.size());
    }

//    @Test
//    public void givenSortFieldWhenSortedThenCorrect() {
//        SimpleSearchApplication simpleSearchApplication
//                = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
//        simpleSearchApplication.createIndex("Ganges", "River in India");
//        simpleSearchApplication.createIndex("Mekong", "This river flows in south Asia");
//        simpleSearchApplication.createIndex("Amazon", "Rain forest river");
//        simpleSearchApplication.createIndex("Rhine", "Belongs to Europe");
//        simpleSearchApplication.createIndex("Nile", "Longest River");
//
//        Term term = new Term("body", "river");
//        Query query = new WildcardQuery(term);
//
//        SortField sortField = new SortField("title", SortField.Type.STRING_VAL, true);
//        Sort sortByTitle = new Sort(sortField);
//
//        List<Document> documents = simpleSearchApplication.searchIndex(query);
//        System.out.println("Documents " + documents);
//        assertEquals(4, documents.size());
//        assertEquals("Amazon", documents.get(0).get("title"));
//    }

    @Test
    public void whenDocumentDeletedThenCorrect() {
        SimpleSearchApplication simpleSearchApplication = new SimpleSearchApplication(new ByteBuffersDirectory(), new StandardAnalyzer());
        simpleSearchApplication.createIndex("Ganges", "River in India");
        simpleSearchApplication.createIndex("Mekong", "This river flows in south Asia");

        Term term = new Term("title", "Ganges");
        simpleSearchApplication.deleteDocument(term);

        Query query = new TermQuery(term);

        List<Document> documents = simpleSearchApplication.searchIndex(query);
        assertEquals(0, documents.size());
    }



}
