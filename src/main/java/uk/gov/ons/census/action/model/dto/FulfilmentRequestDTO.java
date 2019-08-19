package uk.gov.ons.census.action.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class FulfilmentRequestDTO {

  private UUID caseId;

  private String fulfilmentCode;

  private Contact contact;
}
