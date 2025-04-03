package cn.bugstack.xfg.dev.tech.api;

import org.springframework.web.multipart.MultipartFile;

import cn.bugstack.xfg.dev.tech.api.response.Response;
import java.util.List;

public interface IRAGService {

  Response<List<String>> queryRagTagList();

  Response<String> uploadFile(String ragTag, List<MultipartFile> files);

  Response<String> analyzeGitRepository(String repoUrl, String userName,String token) throws Exception;

}
