package cn.bugstack.xfg.dev.tech.api.response;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAnswerData implements Serializable {

  private String question;
  private String answer;
  private String ragTag;
  private String model;
  private Integer retrievedCount;
  private Long latencyMs;
  private List<RagCitation> citations;
}
