package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class Payload {
  private CollectionCase collectionCase;
  private Uac uac;
  private PrintCaseSelected printCaseSelected;
  private FulfilmentRequestDTO fulfilmentRequest;
  private FieldCaseSelected fieldCaseSelected;
}
