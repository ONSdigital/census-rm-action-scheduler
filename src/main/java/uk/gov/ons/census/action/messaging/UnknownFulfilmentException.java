package uk.gov.ons.census.action.messaging;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UnknownFulfilmentException extends Exception {
  public final String fulfilmentCode;
}
