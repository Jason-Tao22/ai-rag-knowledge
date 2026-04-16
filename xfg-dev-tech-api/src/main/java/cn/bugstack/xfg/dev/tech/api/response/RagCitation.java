package cn.bugstack.xfg.dev.tech.api.response;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagCitation implements Serializable {

  private Integer rank;
  private String documentId;
  private String chunkId;
  private String title;
  private String sourceName;
  private String sourceUrl;
  private String sourceType;
  private String filePath;
  private String passage;
  private Double score;
}
