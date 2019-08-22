package uk.gov.ons.census.action.model.dto;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FulfilmentRequestDTO {

  private UUID caseId;

  private String fulfilmentCode;

  private Contact contact;
}
