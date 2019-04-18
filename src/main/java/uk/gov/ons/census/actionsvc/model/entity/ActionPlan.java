package uk.gov.ons.census.actionsvc.model.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;

/** Domain model object. */
@Entity
@Data
public class ActionPlan {

  @Id private UUID id;

  @OneToMany(mappedBy = "actionPlan")
  List<ActionRule> actionRules;
}
