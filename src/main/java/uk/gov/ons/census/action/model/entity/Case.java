package uk.gov.ons.census.action.model.entity;

import java.util.UUID;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
    name = "cases",
    indexes = {
      @Index(name = "receipt_received_idx", columnList = "receipt_received"),
      @Index(name = "case_id_idx", columnList = "case_id", unique = true),
      @Index(name = "treatment_code_idx", columnList = "treatment_code")
    })
public class Case {

  @Id private long caseRef;

  @Column(name = "case_id")
  private UUID caseId;

  @Column private String caseType;

  @Column private String arid;

  @Column private String estabArid;

  @Column private String uprn;

  @Column private String addressType;

  @Column private String estabType;

  @Column private String addressLevel;

  @Column private String abpCode;

  @Column private String organisationName;

  @Column private String addressLine1;

  @Column private String addressLine2;

  @Column private String addressLine3;

  @Column private String townName;

  @Column private String postcode;

  @Column private String latitude;

  @Column private String longitude;

  @Column private String oa;

  @Column private String lsoa;

  @Column private String msoa;

  @Column private String lad;

  @Column private String region;

  @Column private String htcWillingness;

  @Column private String htcDigital;

  @Column private String fieldCoordinatorId;

  @Column private String fieldOfficerId;

  @Column(name = "treatment_code")
  private String treatmentCode;

  @Column private Integer ceExpectedCapacity;

  @Column private Integer ceActualResponses;

  @Column private String collectionExerciseId;

  @Column private String actionPlanId;

  @Column(name = "receipt_received", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean receiptReceived;

  @Column(name = "refusal_received", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean refusalReceived;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean addressInvalid;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean undeliveredAsAddressed;

  @Column(columnDefinition = "BOOLEAN DEFAULT false")
  private boolean handDelivery;
}
