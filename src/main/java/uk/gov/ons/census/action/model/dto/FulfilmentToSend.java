package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class FulfilmentToSend {
  private String fulfilment_code;
//  private String event_payload;
  private Integer count;


}
