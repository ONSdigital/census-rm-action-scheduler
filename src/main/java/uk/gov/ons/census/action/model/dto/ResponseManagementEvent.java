package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class ResponseManagementEvent {
  private Event event;
  private Payload payload;
}
