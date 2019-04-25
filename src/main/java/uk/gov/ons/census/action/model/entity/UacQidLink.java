package uk.gov.ons.census.action.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(indexes = {@Index(columnList="caseId", unique = false)})
public class UacQidLink {
  @Id private UUID id;

  @Column private String qid;

  @Column private String uac;

  @Column private String caseId;
}
