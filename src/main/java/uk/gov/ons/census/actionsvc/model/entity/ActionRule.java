package uk.gov.ons.census.actionsvc.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
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

  @ManyToOne private ActionType actionType;

  @Column private OffsetDateTime triggerDateTime;

  @Column private Boolean hasTriggered;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, List<String>> classifiers;
}
