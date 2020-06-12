package uk.gov.ons.census.action.model.entity;

import java.util.UUID;
import javax.persistence.*;
import lombok.Data;

@Entity
@Data
public class FulfilmentToProcess {

  @Id
  @Column(columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column private String fulfilmentCode;

  @ManyToOne private Case caze;

  @Column
  @Enumerated(EnumType.STRING)
  private ActionType actionType;

  @Column private String addressLine1;

  @Column private String addressLine2;

  @Column private String addressLine3;

  @Column private String townName;

  @Column private String postcode;

  @Column private String title;
  @Column private String forename;
  @Column private String surname;

  @Column private Integer quantity;

  @Column private UUID batchId;
}
