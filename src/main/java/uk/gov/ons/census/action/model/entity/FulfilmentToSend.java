package uk.gov.ons.census.action.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.ons.census.action.model.dto.PrintFileDto;

@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Data
public class FulfilmentToSend {

  @Id
  @Column(columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column private String fulfilmentCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb", nullable = false)
  private PrintFileDto messageData;

  @Column private Integer quantity;

  @Column private UUID batchId;
}
