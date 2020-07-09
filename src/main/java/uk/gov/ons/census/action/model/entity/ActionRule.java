package uk.gov.ons.census.action.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import lombok.Data;
import org.hibernate.annotations.Type;

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

  @Lob
  @Type(type = "org.hibernate.type.BinaryType")
  @Column(nullable = false)
  private byte[] classifiersClause;

  public void setClassifiersClause(String classifierClauseStr) {
    classifiersClause = classifierClauseStr.getBytes();
  }

  public String getClassifiersClause() {
    return new String(classifiersClause);
  }
}
