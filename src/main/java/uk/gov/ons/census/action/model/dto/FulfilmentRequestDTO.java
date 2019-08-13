package uk.gov.ons.census.action.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FulfilmentRequestDTO {

  private String caseId;

  private String fulfilmentCode;

  private Contact contact;
}
