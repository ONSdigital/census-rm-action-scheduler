package uk.gov.ons.census.action.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class UacCreateDTO {

  private UUID caseId;
  private String questionnaireType;
}
