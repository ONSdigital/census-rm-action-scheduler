package uk.gov.ons.census.action.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class ActionRule {

  @Id private UUID id;

  @ManyToOne private ActionPlan actionPlan;

  @Column
  @Enumerated(EnumType.STRING)
  private ActionType actionType;

  @Column(columnDefinition = "timestamp with time zone")
  private OffsetDateTime triggerDateTime;

  @Column private Boolean hasTriggered;

  // This will be set to varchar(max) in the ddl
  @Column(nullable = false, length = 100000)
  private String userDefinedWhereClause;
}
