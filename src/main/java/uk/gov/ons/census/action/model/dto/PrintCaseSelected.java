package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class PrintCaseSelected {
  private long caseRef;
  private String packCode;
  private String actionRuleId;
}
