package uk.gov.ons.census.actionsvc.model.dto;

import lombok.Data;

@Data
public class FanoutEvent {
  private Event event;
  private Payload payload;
}
