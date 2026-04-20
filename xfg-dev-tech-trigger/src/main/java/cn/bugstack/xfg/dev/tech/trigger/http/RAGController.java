package cn.bugstack.xfg.dev.tech.trigger.http;

import cn.bugstack.xfg.dev.tech.api.IRAGService;
import cn.bugstack.xfg.dev.tech.api.response.RagAnswerData;
import cn.bugstack.xfg.dev.tech.api.response.RagCitation;
import cn.bugstack.xfg.dev.tech.api.response.RagImportSummary;
import cn.bugstack.xfg.dev.tech.api.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")
public class RAGController implements IRAGService {

  private static final String DEFAULT_MODEL = "deepseek-r1:1.5b";
  private static final String KNOWLEDGE_METADATA_KEY = "knowledge";
  private static final String DOCUMENT_ID_METADATA_KEY = "document_id";
  private static final String TITLE_METADATA_KEY = "title";
  private static final String SOURCE_URL_METADATA_KEY = "source_url";
  private static final String SOURCE_NAME_METADATA_KEY = "source_name";
  private static final String SOURCE_TYPE_METADATA_KEY = "source_type";
  private static final String FILE_PATH_METADATA_KEY = "file_path";
  private static final String REPO_URL_METADATA_KEY = "repo_url";
  private static final String CHUNK_ID_METADATA_KEY = "chunk_id";
  private static final String CHUNK_INDEX_METADATA_KEY = "chunk_index";
  private static final String SCORE_METADATA_KEY = "distance";
  private static final int IMPORT_BATCH_SIZE = 250;
  private static final String CITATION_PROMPT = """
      You are a retrieval-grounded assistant for technical documentation.
      Use only facts supported by the retrieved passages below.
      If the passages do not contain enough evidence to answer, say that you do not know.
      Respond in the same language as the user's question whenever possible.
      Keep the answer concise and practical, ideally within 2 to 4 sentences.
      Do not reveal chain-of-thought or internal reasoning.
      When you use a retrieved passage, cite it inline with bracketed citations such as [1] or [2].
      Do not invent facts or citations.

      RETRIEVED PASSAGES:
      {documents}
      """;

  @Resource
  private OllamaChatClient ollamaChatClient;
  @Resource
  private TokenTextSplitter tokenTextSplitter;
  @Resource
  private PgVectorStore pgVectorStore;
  @Resource
  private RedissonClient redissonClient;
  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private JdbcTemplate jdbcTemplate;

  @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
  @Override
  public Response<List<String>> queryRagTagList() {
    RList<String> elements = redissonClient.getList("ragTag");
    return Response.<List<String>>builder().code("0000").data(elements).build();
  }

  @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
  @Override
  public Response<String> uploadFile(@RequestParam("ragTag") String ragTag, @RequestParam("file") List<MultipartFile> files) {
    log.info("Starting knowledge-base upload: {}", ragTag);
    int totalChunks = 0;

    for (MultipartFile file : files) {
      TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
      List<Document> documents = documentReader.get();

      Map<String, Object> baseMetadata = buildSourceMetadata(
          ragTag,
          "upload",
          file.getOriginalFilename(),
          file.getOriginalFilename(),
          null,
          sanitizeIdentifier(file.getOriginalFilename()),
          null
      );
      totalChunks += indexDocuments(ragTag, documents, baseMetadata);
    }

    registerRagTag(ragTag);
    log.info("Knowledge-base upload completed: {}, chunks={}", ragTag, totalChunks);
    return Response.<String>builder().code("0000").info("Success").data("Imported " + totalChunks + " chunks").build();
  }

  @RequestMapping(value = "corpus/import_jsonl", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
  public Response<RagImportSummary> importJsonlCorpus(@RequestParam("ragTag") String ragTag,
                                                      @RequestParam("file") MultipartFile file) throws IOException {
    log.info("Starting JSONL corpus import: {}", ragTag);
    ensureCorpusTable();

    int importedDocuments = 0;
    int importedChunks = 0;
    List<CorpusRow> rows = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (StringUtils.isBlank(line)) {
          continue;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> record = objectMapper.readValue(line, LinkedHashMap.class);

        String text = firstNonBlank(asString(record.get("text")), asString(record.get("passage")));
        if (StringUtils.isBlank(text)) {
          log.warn("Skipping line {} because the text field is missing", lineNumber);
          continue;
        }

        String documentId = firstNonBlank(asString(record.get("id")), sanitizeIdentifier(file.getOriginalFilename()) + "-doc-" + lineNumber);
        String title = firstNonBlank(asString(record.get("title")), documentId);
        String sourceUrl = firstNonBlank(asString(record.get("url")), asString(record.get("source_url")));
        String sourceName = firstNonBlank(asString(record.get("source")), title);

        Map<String, Object> extraMetadata = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(asString(record.get("file_path")))) {
          extraMetadata.put(FILE_PATH_METADATA_KEY, asString(record.get("file_path")));
        }
        extraMetadata.put("raw_record_id", documentId);
        rows.add(CorpusRow.builder()
            .ragTag(ragTag)
            .documentId(documentId)
            .title(title)
            .sourceName(sourceName)
            .sourceUrl(sourceUrl)
            .sourceType("jsonl")
            .content(text)
            .filePath(asString(extraMetadata.get(FILE_PATH_METADATA_KEY)))
            .build());
        importedDocuments++;

        if (rows.size() >= IMPORT_BATCH_SIZE) {
          importedChunks += batchUpsertCorpusRows(rows);
          rows.clear();
        }
      }
    }

    if (!rows.isEmpty()) {
      importedChunks += batchUpsertCorpusRows(rows);
    }

    registerRagTag(ragTag);
    log.info("JSONL import completed: {}, docs={}, chunks={}", ragTag, importedDocuments, importedChunks);

    return Response.<RagImportSummary>builder()
        .code("0000")
        .info("Success")
        .data(RagImportSummary.builder()
            .ragTag(ragTag)
            .sourceFile(file.getOriginalFilename())
            .importedDocuments(importedDocuments)
            .importedChunks(importedChunks)
            .build())
        .build();
  }

  @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
  @Override
  public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl,
                                               @RequestParam("userName") String userName,
                                               @RequestParam("token") String token) throws Exception {
    String localPath = "./git-cloned-repo";
    String repoProjectName = extractProjectName(repoUrl);
    log.info("Clone path: {}", new File(localPath).getAbsolutePath());

    FileUtils.deleteDirectory(new File(localPath));

    Git git = Git.cloneRepository()
        .setURI(repoUrl)
        .setDirectory(new File(localPath))
        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
        .call();

    AtomicInteger importedChunks = new AtomicInteger();
    Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.info("{} parsing repository file for knowledge-base import: {}", repoProjectName, file.getFileName());
        try {
          TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
          List<Document> documents = reader.get();
          String relativePath = Paths.get(localPath).relativize(file).toString();

          Map<String, Object> extraMetadata = new LinkedHashMap<>();
          extraMetadata.put(FILE_PATH_METADATA_KEY, relativePath);
          extraMetadata.put(REPO_URL_METADATA_KEY, repoUrl);

          importedChunks.addAndGet(indexDocuments(
              repoProjectName,
              documents,
              buildSourceMetadata(
                  repoProjectName,
                  "git",
                  relativePath,
                  file.getFileName().toString(),
                  repoUrl,
                  sanitizeIdentifier(relativePath),
                  extraMetadata
              )
          ));
        } catch (Exception e) {
          log.error("Failed to parse repository file for knowledge-base import: {}", file.getFileName(), e);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.info("Failed to access file: {} - {}", file, exc.getMessage());
        return FileVisitResult.CONTINUE;
      }
    });

    FileUtils.deleteDirectory(new File(localPath));
    registerRagTag(repoProjectName);

    git.close();
    log.info("Repository import completed: {}, chunks={}", repoUrl, importedChunks.get());
    return Response.<String>builder().code("0000").info("Success").data("Imported " + importedChunks.get() + " chunks").build();
  }

  @RequestMapping(value = "query", method = RequestMethod.GET)
  public Response<RagAnswerData> query(@RequestParam("ragTag") String ragTag,
                                       @RequestParam("message") String message,
                                       @RequestParam(value = "model", required = false) String model,
                                       @RequestParam(value = "topK", defaultValue = "5") int topK) {
    long startTime = System.currentTimeMillis();
    String resolvedModel = resolveModel(model);

    try {
      List<RagCitation> citations = retrieveCitations(ragTag, message, topK);

      String answer;
      if (citations.isEmpty()) {
        answer = "I do not know based on the currently indexed passages.";
      } else {
        List<RagCitation> promptCitations = citations.subList(0, Math.min(citations.size(), 3));
        String retrievedPassages = buildPromptContext(promptCitations);
        Message systemMessage = new SystemPromptTemplate(CITATION_PROMPT).createMessage(Map.of("documents", retrievedPassages));
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(new UserMessage(message));

        ChatResponse response = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create()
            .withModel(resolvedModel)
            .withTemperature(0.1f)
            .withNumPredict(160)));
        answer = extractAnswer(response);
      }

      return Response.<RagAnswerData>builder()
          .code("0000")
          .info("Success")
          .data(RagAnswerData.builder()
              .question(message)
              .answer(answer)
              .ragTag(ragTag)
              .model(resolvedModel)
              .retrievedCount(citations.size())
              .latencyMs(System.currentTimeMillis() - startTime)
              .citations(citations)
              .build())
          .build();
    } catch (Exception e) {
      log.error("RAG query failed: ragTag={}, message={}", ragTag, message, e);
      return Response.<RagAnswerData>builder()
          .code("9999")
          .info("Request processing error: " + e.getMessage())
          .data(RagAnswerData.builder()
              .question(message)
              .answer("The system could not generate an answer for this request.")
              .ragTag(ragTag)
              .model(resolvedModel)
              .retrievedCount(0)
              .latencyMs(System.currentTimeMillis() - startTime)
              .citations(List.of())
              .build())
          .build();
    }
  }

  @RequestMapping(value = "retrieve", method = RequestMethod.GET)
  public Response<RagAnswerData> retrieve(@RequestParam("ragTag") String ragTag,
                                          @RequestParam("message") String message,
                                          @RequestParam(value = "topK", defaultValue = "5") int topK) {
    long startTime = System.currentTimeMillis();

    try {
      List<RagCitation> citations = retrieveCitations(ragTag, message, topK);
      String answer = citations.isEmpty()
          ? "No supporting passages were found for this question."
          : "Retrieved supporting passages below. Use the citations to inspect the source article and verify the evidence.";

      return Response.<RagAnswerData>builder()
          .code("0000")
          .info("Success")
          .data(RagAnswerData.builder()
              .question(message)
              .answer(answer)
              .ragTag(ragTag)
              .model("retrieval-only")
              .retrievedCount(citations.size())
              .latencyMs(System.currentTimeMillis() - startTime)
              .citations(citations)
              .build())
          .build();
    } catch (Exception e) {
      log.error("RAG retrieval failed: ragTag={}, message={}", ragTag, message, e);
      return Response.<RagAnswerData>builder()
          .code("9999")
          .info("Retrieval request processing error: " + e.getMessage())
          .data(RagAnswerData.builder()
              .question(message)
              .answer("The system could not retrieve supporting passages for this request.")
              .ragTag(ragTag)
              .model("retrieval-only")
              .retrievedCount(0)
              .latencyMs(System.currentTimeMillis() - startTime)
              .citations(List.of())
              .build())
          .build();
    }
  }

  private String extractProjectName(String repoUrl) {
    String[] parts = repoUrl.split("/");
    String projectNameWithGit = parts[parts.length - 1];
    return projectNameWithGit.replace(".git", "");
  }

  private int indexDocuments(String ragTag, List<Document> documents, Map<String, Object> baseMetadata) {
    List<Document> splitDocuments = prepareSplitDocuments(ragTag, documents, baseMetadata);
    if (!splitDocuments.isEmpty()) {
      pgVectorStore.accept(splitDocuments);
    }

    return splitDocuments.size();
  }

  private List<Document> prepareSplitDocuments(String ragTag, List<Document> documents, Map<String, Object> baseMetadata) {
    List<Document> splitDocuments = tokenTextSplitter.apply(documents);
    int chunkIndex = 0;

    for (Document splitDocument : splitDocuments) {
      splitDocument.getMetadata().putAll(baseMetadata);
      splitDocument.getMetadata().put(KNOWLEDGE_METADATA_KEY, ragTag);
      splitDocument.getMetadata().put(CHUNK_INDEX_METADATA_KEY, chunkIndex);
      splitDocument.getMetadata().put(CHUNK_ID_METADATA_KEY, baseMetadata.get(DOCUMENT_ID_METADATA_KEY) + "#chunk-" + chunkIndex);
      chunkIndex++;
    }

    return splitDocuments;
  }

  private void ensureCorpusTable() {
    jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS rag_corpus_documents (
            rag_tag VARCHAR(128) NOT NULL,
            document_id VARCHAR(255) NOT NULL,
            title TEXT,
            source_name TEXT,
            source_url TEXT,
            source_type TEXT,
            file_path TEXT,
            content TEXT NOT NULL,
            search_vector tsvector GENERATED ALWAYS AS (
                to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, ''))
            ) STORED,
            PRIMARY KEY (rag_tag, document_id)
        )
        """);
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_corpus_documents_rag_tag ON rag_corpus_documents (rag_tag)");
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_corpus_documents_search_vector ON rag_corpus_documents USING GIN (search_vector)");
  }

  private int batchUpsertCorpusRows(List<CorpusRow> rows) {
    jdbcTemplate.batchUpdate("""
            INSERT INTO rag_corpus_documents (
                rag_tag,
                document_id,
                title,
                source_name,
                source_url,
                source_type,
                file_path,
                content
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (rag_tag, document_id) DO UPDATE SET
                title = EXCLUDED.title,
                source_name = EXCLUDED.source_name,
                source_url = EXCLUDED.source_url,
                source_type = EXCLUDED.source_type,
                file_path = EXCLUDED.file_path,
                content = EXCLUDED.content
            """,
        rows,
        rows.size(),
        (ps, row) -> {
          ps.setString(1, row.getRagTag());
          ps.setString(2, row.getDocumentId());
          ps.setString(3, row.getTitle());
          ps.setString(4, row.getSourceName());
          ps.setString(5, row.getSourceUrl());
          ps.setString(6, row.getSourceType());
          ps.setString(7, row.getFilePath());
          ps.setString(8, row.getContent());
        });
    return rows.size();
  }

  private List<RagCitation> retrieveCitations(String ragTag, String message, int topK) {
    int normalizedTopK = Math.max(1, Math.min(topK, 10));
    List<RagCitation> citations = searchCorpusCitations(ragTag, message, normalizedTopK);
    if (!citations.isEmpty()) {
      return citations;
    }

    SearchRequest request = SearchRequest.query(message)
        .withTopK(normalizedTopK)
        .withFilterExpression(KNOWLEDGE_METADATA_KEY + " == '" + escapeFilterValue(ragTag) + "'");

    List<Document> documents = pgVectorStore.similaritySearch(request);
    return buildCitations(documents);
  }

  private List<RagCitation> searchCorpusCitations(String ragTag, String message, int topK) {
    ensureCorpusTable();

    List<RagCitation> citations = jdbcTemplate.query("""
            SELECT
                document_id,
                title,
                source_name,
                source_url,
                source_type,
                file_path,
                ts_headline(
                    'english',
                    content,
                    websearch_to_tsquery('english', ?),
                    'MaxFragments=2,MaxWords=40,MinWords=10'
                ) AS snippet,
                ts_rank_cd(search_vector, websearch_to_tsquery('english', ?)) AS score
            FROM rag_corpus_documents
            WHERE rag_tag = ?
              AND search_vector @@ websearch_to_tsquery('english', ?)
            ORDER BY score DESC, document_id ASC
            LIMIT ?
            """,
        ps -> {
          ps.setString(1, message);
          ps.setString(2, message);
          ps.setString(3, ragTag);
          ps.setString(4, message);
          ps.setInt(5, topK);
        },
        (rs, rowNum) -> RagCitation.builder()
            .rank(rowNum + 1)
            .documentId(rs.getString("document_id"))
            .chunkId(rs.getString("document_id"))
            .title(firstNonBlank(rs.getString("title"), rs.getString("source_name"), "Untitled Source"))
            .sourceName(firstNonBlank(rs.getString("source_name"), rs.getString("title")))
            .sourceUrl(rs.getString("source_url"))
            .sourceType(rs.getString("source_type"))
            .filePath(rs.getString("file_path"))
            .passage(firstNonBlank(rs.getString("snippet"), "No supporting passage available."))
            .score(rs.getDouble("score"))
            .build()
    );

    if (!citations.isEmpty()) {
      return citations;
    }

    String likePattern = "%" + message.replace("%", " ").replace("_", " ").trim() + "%";
    return jdbcTemplate.query("""
            SELECT
                document_id,
                title,
                source_name,
                source_url,
                source_type,
                file_path,
                content
            FROM rag_corpus_documents
            WHERE rag_tag = ?
              AND (title ILIKE ? OR content ILIKE ?)
            LIMIT ?
            """,
        ps -> {
          ps.setString(1, ragTag);
          ps.setString(2, likePattern);
          ps.setString(3, likePattern);
          ps.setInt(4, topK);
        },
        (rs, rowNum) -> RagCitation.builder()
            .rank(rowNum + 1)
            .documentId(rs.getString("document_id"))
            .chunkId(rs.getString("document_id"))
            .title(firstNonBlank(rs.getString("title"), rs.getString("source_name"), "Untitled Source"))
            .sourceName(firstNonBlank(rs.getString("source_name"), rs.getString("title")))
            .sourceUrl(rs.getString("source_url"))
            .sourceType(rs.getString("source_type"))
            .filePath(rs.getString("file_path"))
            .passage(StringUtils.abbreviate(StringUtils.normalizeSpace(rs.getString("content")), 1200))
            .score(null)
            .build()
    );
  }

  private Map<String, Object> buildSourceMetadata(String ragTag,
                                                  String sourceType,
                                                  String sourceName,
                                                  String title,
                                                  String sourceUrl,
                                                  String documentId,
                                                  Map<String, Object> extraMetadata) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(KNOWLEDGE_METADATA_KEY, ragTag);
    metadata.put(SOURCE_TYPE_METADATA_KEY, firstNonBlank(sourceType, "unknown"));
    metadata.put(SOURCE_NAME_METADATA_KEY, firstNonBlank(sourceName, title, documentId));
    metadata.put(TITLE_METADATA_KEY, firstNonBlank(title, sourceName, documentId));
    metadata.put(DOCUMENT_ID_METADATA_KEY, firstNonBlank(documentId, sanitizeIdentifier(sourceName)));

    if (StringUtils.isNotBlank(sourceUrl)) {
      metadata.put(SOURCE_URL_METADATA_KEY, sourceUrl);
    }

    if (extraMetadata != null) {
      metadata.putAll(extraMetadata);
    }

    return metadata;
  }

  private List<RagCitation> buildCitations(List<Document> documents) {
    List<RagCitation> citations = new ArrayList<>();

    for (int i = 0; i < documents.size(); i++) {
      Document document = documents.get(i);
      Map<String, Object> metadata = document.getMetadata();

      citations.add(RagCitation.builder()
          .rank(i + 1)
          .documentId(firstNonBlank(asString(metadata.get(DOCUMENT_ID_METADATA_KEY)), document.getId()))
          .chunkId(firstNonBlank(asString(metadata.get(CHUNK_ID_METADATA_KEY)), document.getId()))
          .title(firstNonBlank(asString(metadata.get(TITLE_METADATA_KEY)), asString(metadata.get(SOURCE_NAME_METADATA_KEY)), "Untitled Source"))
          .sourceName(firstNonBlank(asString(metadata.get(SOURCE_NAME_METADATA_KEY)), asString(metadata.get(TITLE_METADATA_KEY))))
          .sourceUrl(asString(metadata.get(SOURCE_URL_METADATA_KEY)))
          .sourceType(asString(metadata.get(SOURCE_TYPE_METADATA_KEY)))
          .filePath(asString(metadata.get(FILE_PATH_METADATA_KEY)))
          .passage(StringUtils.abbreviate(StringUtils.normalizeSpace(document.getContent()), 1200))
          .score(asDouble(metadata.get(SCORE_METADATA_KEY)))
          .build());
    }

    return citations;
  }

  private String buildPromptContext(List<RagCitation> citations) {
    return citations.stream()
        .map(citation -> {
          StringBuilder builder = new StringBuilder();
          builder.append("[").append(citation.getRank()).append("]\n");
          builder.append("Title: ").append(firstNonBlank(citation.getTitle(), "Untitled Source")).append("\n");
          if (StringUtils.isNotBlank(citation.getSourceName())) {
            builder.append("Source: ").append(citation.getSourceName()).append("\n");
          }
          if (StringUtils.isNotBlank(citation.getSourceUrl())) {
            builder.append("URL: ").append(citation.getSourceUrl()).append("\n");
          }
          builder.append("Passage: ").append(citation.getPassage()).append("\n");
          return builder.toString();
        })
        .collect(Collectors.joining("\n"));
  }

  private void registerRagTag(String ragTag) {
    RList<String> elements = redissonClient.getList("ragTag");
    if (!elements.contains(ragTag)) {
      elements.add(ragTag);
    }
  }

  private String extractAnswer(ChatResponse response) {
    if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
      return "The model returned an empty answer.";
    }

    return firstNonBlank(response.getResult().getOutput().getContent(), "The model returned an empty answer.");
  }

  private String resolveModel(String model) {
    return StringUtils.isBlank(model) ? DEFAULT_MODEL : model.trim();
  }

  private String escapeFilterValue(String input) {
    return input.replace("'", "''");
  }

  private String sanitizeIdentifier(String input) {
    if (StringUtils.isBlank(input)) {
      return "source";
    }

    String normalized = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._/-]+", "-");
    return normalized.replace('/', '-');
  }

  private String firstNonBlank(String... candidates) {
    for (String candidate : candidates) {
      if (StringUtils.isNotBlank(candidate)) {
        return candidate.trim();
      }
    }
    return null;
  }

  @lombok.Builder
  @lombok.Getter
  private static class CorpusRow {
    private String ragTag;
    private String documentId;
    private String title;
    private String sourceName;
    private String sourceUrl;
    private String sourceType;
    private String filePath;
    private String content;
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Double asDouble(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
