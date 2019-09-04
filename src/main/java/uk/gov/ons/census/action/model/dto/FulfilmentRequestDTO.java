package uk.gov.ons.census.action.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FulfilmentRequestDTO {
  private UUID caseId;
  private String fulfilmentCode;
  private Contact contact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID individualCaseId;
}
