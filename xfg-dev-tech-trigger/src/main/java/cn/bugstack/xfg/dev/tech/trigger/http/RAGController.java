package cn.bugstack.xfg.dev.tech.trigger.http;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import cn.bugstack.xfg.dev.tech.api.IRAGService;
import cn.bugstack.xfg.dev.tech.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")

public class RAGController implements IRAGService {
  @Resource
  private OllamaChatClient ollamaChatClient;
  @Resource
  private TokenTextSplitter tokenTextSplitter;
  @Resource
  private PgVectorStore pgVectorStore;
  @Resource
  private RedissonClient redissonClient;


  @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
  @Override
  public Response<List<String>> queryRagTagList() {
    RList<String> elements = redissonClient.getList("ragTag");
    return Response.<List<String>>builder().code("0000").data(elements).build();
  }


  @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
  @Override
  public Response<String> uploadFile(@RequestParam String ragTag, @RequestParam("file") List<MultipartFile> files) {
    log.info("上传知识库开始{}",ragTag);
    for(MultipartFile file:files){
      TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
      List<Document> documents = documentReader.get();
      List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

      documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
      documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
      pgVectorStore.accept(documentSplitterList);

      RList<Object> elements = redissonClient.getList("ragTag");
      if(!elements.contains(ragTag)){
        elements.add(ragTag);
      }
    }
    log.info("知识库上传完成",ragTag);
    return Response.<String>builder().code("0000").info("调用成功").build();
  }


  @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
  @Override
  public Response<String> analyzeGitRepository(@RequestParam String repoUrl, @RequestParam String userName, @RequestParam String token) throws Exception {
    String localPath = "./git-cloned-repo";
    String repoProjectName = extractProjectName(repoUrl);
    log.info("克隆路径：{}", new File(localPath).getAbsolutePath());

    FileUtils.deleteDirectory(new File(localPath));

    Git git = Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(new File(localPath))
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
            .call();

    // 使用Files.walkFileTree遍历目录
    Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
        try {
          TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
          List<Document> documents = reader.get();
          List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

          documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

          documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

          pgVectorStore.accept(documentSplitterList);
        } catch (Exception e) {
          log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
        }

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
        return FileVisitResult.CONTINUE;
      }
    });

    FileUtils.deleteDirectory(new File(localPath));

    // 添加知识库记录
    RList<String> elements = redissonClient.getList("ragTag");
    if (!elements.contains(repoProjectName)) {
      elements.add(repoProjectName);
    }

    git.close();
    log.info("遍历解析路径，上传完成:{}", repoUrl);
    return Response.<String>builder().code("0000").info("调用成功").build();
  }

  private String extractProjectName(String repoUrl) {
    String[] parts = repoUrl.split("/");
    String projectNameWithGit = parts[parts.length - 1];
    return projectNameWithGit.replace(".git", "");
  }
}
