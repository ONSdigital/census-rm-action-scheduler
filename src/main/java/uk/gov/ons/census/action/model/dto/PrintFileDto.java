package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class PrintFileDto {
  private String uac;
  private String qid;
  private String uacWales;
  private String qidWales;
  private long caseRef;
  private String title;
  private String forename;
  private String surname;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String packCode;
  private String actionType;
  private String fieldCoordinatorId;
}
