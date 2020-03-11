package uk.gov.ons.census.action.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(
    indexes = {
      @Index(name = "uacqid_case_id_idx", columnList = "case_id"),
      @Index(name = "qid_idx", columnList = "qid")
    })
public class UacQidLink {
  @Id private UUID id;

  @Column(name = "qid")
  private String qid;

  @Column private String uac;

  @Column(name = "case_id")
  private String caseId;

  @Column private boolean active;
}
