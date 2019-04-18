package uk.gov.ons.census.actionsvc.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class UacQidLink {
  @Id private UUID id;

  @Column private String qid;

  @Column private String uac;

  @ManyToOne private Case caze;
}
