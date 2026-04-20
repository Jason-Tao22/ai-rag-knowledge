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
public class RagImportSummary implements Serializable {

  private String ragTag;
  private String sourceFile;
  private Integer importedDocuments;
  private Integer importedChunks;
}
