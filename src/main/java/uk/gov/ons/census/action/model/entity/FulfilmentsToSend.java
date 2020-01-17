package uk.gov.ons.census.action.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Data
public class FulfilmentsToSend {

  @Id
  @Column(columnDefinition = "serial")
  private long id;

  @Column private String fulfilmentCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb", nullable = false)
  private String messageData;

  @Column(nullable = true)
  private Integer quantity;

  @Column(nullable = true)
  private UUID batchId;
}
