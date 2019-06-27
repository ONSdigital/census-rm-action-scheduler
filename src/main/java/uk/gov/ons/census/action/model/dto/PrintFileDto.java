package uk.gov.ons.census.action.model.dto;

import lombok.Data;

@Data
public class PrintFileDto {
    private String iac;
    private String qid;
    private String iacWales;
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
    private String batchId;
    private String batchQty;
    private String actionType;
}
