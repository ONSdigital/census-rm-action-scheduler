package uk.gov.ons.census.action.model.entity;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class CaseToProcess {
  @Id private UUID id;

  @ManyToOne private Case caze;

  @ManyToOne private ActionRule actionRule;

  private UUID batchId;
  private int batchQuantity;
}
