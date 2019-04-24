package uk.gov.ons.census.action.model.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class ActionType {

  @Id private UUID id;

  @Column private String name;

  @Column private String description;

  @Column private String handler;

  @OneToMany(mappedBy = "actionType")
  List<ActionRule> actionRules;
}
