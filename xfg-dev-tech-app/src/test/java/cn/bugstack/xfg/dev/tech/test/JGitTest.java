package cn.bugstack.xfg.dev.tech.test;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;



import jakarta.annotation.Resource;
import jodd.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JGitTest {

  @Resource
  private OllamaChatClient ollamaChatClient;
  @Resource
  private TokenTextSplitter tokenTextSplitter;
  @Resource
  private SimpleVectorStore simpleVectorStore;
  @Resource
  private PgVectorStore pgVectorStore;

  @Test
  public void test() throws IOException, GitAPIException {
    String repoURL = "https://github.com/Jason-Tao22/SmartCampus";
    String username = "Jason-Tao22";
    String password = "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";

    String localPath = "./cloned-repo";
    log.info("克隆地址"+new File(localPath).getAbsolutePath());
    FileUtils.deleteDirectory(new File(localPath));
    Git git = Git.cloneRepository()
            .setURI(repoURL)
            .setDirectory(new File(localPath))
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
            .call();
    git.close();
  }

  @Test
  public  void test_file() throws IOException {
    // 使用Files.walkFileTree遍历目录
    Files.walkFileTree(Paths.get("./cloned-repo"), new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try {
          log.info("文件路径:{}" + file.toString());
          /**
          PathResource resource = new PathResource(file);
          TikaDocumentReader reader = new TikaDocumentReader(resource);
          List<Document> documents = reader.get();
          List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

          documents.forEach(doc -> doc.getMetadata().put("knowledge", "SmartCampus"));

          documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "SmartCampus"));

          pgVectorStore.accept(documentSplitterList);
           */
        } catch (Exception e) {
          log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }
}
