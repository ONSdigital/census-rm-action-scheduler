package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class FieldworkFollowup {
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String organisationName;
  private String arid;
  private String uprn;
  private String oa;
  private String latitude;
  private String longitude;
  private String actionPlan;
  private String actionType;
  private String caseId;
  private String caseRef;
  private String uac;
  private String addressType;
  private String addressLevel;
  private String treatmentCode;
  private String fieldOfficerId;
  private String fieldCoordinatorId;
  private String ceExpectedCapacity;
  private String surveyName;
  private Boolean undeliveredAsAddress;
  private Boolean blankQreReturned;
}
