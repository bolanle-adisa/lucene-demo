//import org.apache.lucene.document.*;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.ByteBuffersDirectory;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.index.IndexWriterConfig;
//import org.apache.lucene.util.BytesRef;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class wikiTest {
//
//    private Directory directory;
//    private StandardAnalyzer analyzer;
//
//    @BeforeEach
//    public void setUp() throws Exception {
//        // Create an in-memory directory for testing
//        directory = new ByteBuffersDirectory();
//        analyzer = new StandardAnalyzer();
//
//        // Index sample documents for testing
//        indexSampleDocuments();
//    }
//
//    @Test
//    public void testSearchIndex() throws Exception {
//        String field = "content";
//        String queryString = "physical";
//
//        List<Document> searchResults = wiki.searchIndex(directory, analyzer, field, queryString);
//
//        // Verify the number of search results
//        assertEquals(1, searchResults.size());
//
//        // Verify the content of the search result
//        Document result = searchResults.get(0);
//        assertEquals("Physical fitness", result.get("title"));
//    }
//
//    @Test
//    public void testSearchIndexWithPhrase() throws Exception {
//        String field = "content";
//        String[] terms = {"physical", "fitness"};
//
//        List<Document> searchResults = wiki.searchIndexWithPhrase(directory, analyzer, field, terms);
//
//        // Verify the number of search results
//        assertEquals(1, searchResults.size());
//
//        // Verify the content of the search result
//        Document result = searchResults.get(0);
//        assertEquals("Physical fitness", result.get("title"));
//    }
//
//    @Test
//    public void testDeleteDocumentByTitle() throws Exception {
//        // Create an instance of your Wiki class
//        wiki wiki = new wiki();
//
//        // Set up the Lucene environment
//        Directory directory = new ByteBuffersDirectory();
//        StandardAnalyzer analyzer = new StandardAnalyzer();
//
//        // Initialize the IndexWriter
//        wiki.initializeWriter(directory, analyzer);
//
//        // Wikipedia page titles to index
//        String[] pageTitles = {"Physical fitness", "Culture", "Climate", "Atmosphere"};
//
//        // Index the documents
//        wiki.indexDocuments(analyzer, pageTitles);
//
//        String titleToDelete = "Culture";
//
//        // Delete the document
//        wiki.deleteDocumentByTitle(titleToDelete);
//
//        // Perform a search operation
//        String searchQuery = "physical"; // The search query
//        List<Document> searchResults = wiki.searchIndex(directory, analyzer, "content", searchQuery);
//
//        // Verify the search results
//        boolean documentFound = false;
//        for (Document result : searchResults) {
//            if (result.get("title").equals(titleToDelete)) {
//                documentFound = true;
//                break;
//            }
//        }
//
//        // Assert that the deleted document is no longer present in the search results
//        assertFalse(documentFound);
//
//        // Close the IndexWriter
//        wiki.writer.close();
//    }
//
//    private void indexSampleDocuments() throws Exception {
//        // Create IndexWriter
//        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
//
//            // Create sample documents
//            Document doc1 = new Document();
//            doc1.add(new StringField("title", "Physical fitness", Field.Store.YES));
//            doc1.add(new SortedDocValuesField("title", new BytesRef("Physical fitness")));
//            doc1.add(new TextField("content", "Physical fitness is important for overall well-being.", Field.Store.YES));
//
//            Document doc2 = new Document();
//            doc2.add(new StringField("title", "Culture", Field.Store.YES));
//            doc2.add(new SortedDocValuesField("title", new BytesRef("Culture")));
//            doc2.add(new TextField("content", "Culture encompasses the social behavior and norms found in human societies.", Field.Store.YES));
//
//            // Add documents to the index
//            writer.addDocument(doc1);
//            writer.addDocument(doc2);
//
//            // Commit the changes
//            writer.commit();
//        }
//    }
//}