package uk.gov.ons.census.action.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, List<String>> classifiers;
}
