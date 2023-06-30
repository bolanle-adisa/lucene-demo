import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class wiki {
    private static final Logger logger = LoggerFactory.getLogger(wiki.class);

    // Directories for each language
    static Map<String, Directory> languageDirectories = new HashMap<>();

    // Index writers for each language
    static Map<String, IndexWriter> languageWriters = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Set up Lucene environment
        setupDirectoriesAndWriters();

        // Wikipedia page titles to index
        String[] englishPageTitles = {"Physical fitness", "Culture", "Climate", "Atmosphere"};
        String[] japanesePageTitles = {"夏", "中国暦", "雨水"};
        String[] germanPageTitles = {"Anamnesis", "Transzendenz", "Psychologie"};

        // Index the documents
        indexDocuments(englishPageTitles, "English");
        indexDocuments(japanesePageTitles, "Japanese");
        indexDocuments(germanPageTitles, "German");

        // Perform the initial search operation
        String searchQueryEng = "region";
        String searchQueryJpn = "雨水";
        String searchQueryGer = "Bedingungen";
        List<Document> initialSearchResultsEng = searchIndex(searchQueryEng, "English");
        List<Document> initialSearchResultsJpn = searchIndex(searchQueryJpn, "Japanese");
        List<Document> initialSearchResultsGer = searchIndex(searchQueryGer, "German");

        // Print the initial search results
        printSearchResults("English", searchQueryEng, initialSearchResultsEng);
        printSearchResults("Japanese", searchQueryJpn, initialSearchResultsJpn);
        printSearchResults("German", searchQueryGer, initialSearchResultsGer);

        // Perform a phrase search operation
        String[] searchTermsEng = {"physical", "fitness"}; // The search phrase in English
        String[] searchTermsJpn = {"夏（なつ", "は"}; // The search phrase in Japanese
        String[] searchTermsGer = {"Zugriff", "darauf"}; // The search phrase in German
        List<Document> searchPhraseResultsEng = searchIndexWithPhrase(searchTermsEng, "English");
        List<Document> searchPhraseResultsJpn = searchIndexWithPhrase(searchTermsJpn, "Japanese");
        List<Document> searchPhraseResultsGer = searchIndexWithPhrase(searchTermsGer, "German");

        // Print the search results for Phrase
        printSearchResultsForPhrase("English", searchPhraseResultsEng);
        printSearchResultsForPhrase("Japanese", searchPhraseResultsJpn);
        printSearchResultsForPhrase("German", searchPhraseResultsGer);

        // Close the IndexWriters after all operations are completed
        closeWriters();
    }

    private static void setupDirectoriesAndWriters() throws IOException {
        languageDirectories.put("English", new ByteBuffersDirectory());
        languageDirectories.put("Japanese", new ByteBuffersDirectory());
        languageDirectories.put("German", new ByteBuffersDirectory());

        StandardAnalyzer englishAnalyzer = new StandardAnalyzer();
        JapaneseAnalyzer japaneseAnalyzer = new JapaneseAnalyzer();
        GermanAnalyzer germanAnalyzer = new GermanAnalyzer();

        languageWriters.put("English", new IndexWriter(languageDirectories.get("English"), new IndexWriterConfig(englishAnalyzer)));
        languageWriters.put("Japanese", new IndexWriter(languageDirectories.get("Japanese"), new IndexWriterConfig(japaneseAnalyzer)));
        languageWriters.put("German", new IndexWriter(languageDirectories.get("German"), new IndexWriterConfig(germanAnalyzer)));
    }

    private static void closeWriters() throws IOException {
        for (IndexWriter writer : languageWriters.values()) {
            writer.close();
        }
    }

    private static void printSearchResults(String language, String query, List<Document> results) {
        logger.info(language + " Search Results for Query '" + query + "':");
        for (Document result : results) {
            logger.info("Title: " + result.get("title"));
            logger.info("Content: " + result.get("content"));
        }
    }

    private static void printSearchResultsForPhrase(String language, List<Document> results) {
        logger.info(language + " Search Results for Phrase Query:");
        for (Document result : results) {
            logger.info("Title: " + result.get("title"));
            logger.info("Content: " + result.get("content"));
        }
    }

    public static void indexDocuments(String[] pageTitles, String language) throws Exception {
        Analyzer analyzer = getAnalyzerForLanguage(language);
        IndexWriter writer = languageWriters.get(language);

        // For each page title, get the page content and index the document
        for (String pageTitle : pageTitles) {
            // Fetch the page content
            String pageContent = fetchWikipediaPageContent(pageTitle, language);
            // Index the document
            Document doc = new Document();
            doc.add(new TextField("title", pageTitle, Field.Store.YES));
            doc.add(new TextField("content", pageContent, Field.Store.YES));
            writer.addDocument(doc);
        }
        // Commit the changes but don't close the writer
        writer.commit();
    }

    public static List<Document> searchIndex(String queryString, String language) throws IOException, ParseException {
        Analyzer analyzer = getAnalyzerForLanguage(language);
        QueryParser queryParser = new QueryParser("content", analyzer);
        Query query = queryParser.parse(queryString);

        IndexReader indexReader = DirectoryReader.open(languageDirectories.get(language));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        List<Document> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = searcher.doc(docId);
            searchResults.add(document);
        }

        indexReader.close();

        return searchResults;
    }

    public static List<Document> searchIndexWithPhrase(String[] terms, String language) throws IOException {
        Analyzer analyzer = getAnalyzerForLanguage(language);

        IndexReader indexReader = DirectoryReader.open(languageDirectories.get(language));
        IndexSearcher searcher = new IndexSearcher(indexReader);

        // Tokenize the search terms using the respective analyzer
        List<String> tokenizedTerms = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(String.join(" ", terms)));
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokenizedTerms.add(charTermAttribute.toString());
        }
        tokenStream.end();
        tokenStream.close();

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        for (String term : tokenizedTerms) {
            builder.add(new Term("content", term));
        }
        PhraseQuery pq = builder.build();

        TopDocs topDocs = searcher.search(pq, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        List<Document> searchResults = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = searcher.doc(docId);
            searchResults.add(document);
        }

        indexReader.close();

        return searchResults;
    }

    public static String fetchWikipediaPageContent(String pageTitle, String language) throws Exception {
        String langCode = "en";
        switch (language) {
            case "Japanese":
                langCode = "ja";
                break;
            case "German":
                langCode = "de";
                break;
        }

        String apiUrlString = "https://" + langCode + ".wikipedia.org/w/api.php?" +
                "format=json&action=query&prop=extracts&exintro&explaintext&redirects=1&titles=" +
                URLEncoder.encode(pageTitle, "UTF-8");

        URL apiUrl = new URL(apiUrlString);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();

        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            throw new Exception("Error: API request failed with response code " + responseCode);
        } else {
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonStringBuilder.toString());
            JsonNode pages = jsonNode.get("query").get("pages");
            JsonNode page = pages.elements().next();
            JsonNode extract = page.get("extract");

            return extract.asText();
        }
    }

    public static Analyzer getAnalyzerForLanguage(String language) {
        switch (language) {
            case "Japanese":
                return new JapaneseAnalyzer();
            case "German":
                return new GermanAnalyzer();
            default:
                return new StandardAnalyzer();
        }
    }
}
