package uk.gov.ons.census.action.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FulfilmentCodeCount {
  private String fulfilmentCode;
  private Long count;
}
