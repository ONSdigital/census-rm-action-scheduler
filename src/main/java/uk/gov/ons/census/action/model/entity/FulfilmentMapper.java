package uk.gov.ons.census.action.model.entity;

import lombok.Data;

@Data
public class FulfilmentMapper {
  private String fulfilmentCode;
  private Long count;

  public FulfilmentMapper(String fulfilmentCode, Long count) {
    this.fulfilmentCode = fulfilmentCode;
    this.count = count;
  }
}
