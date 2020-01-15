package uk.gov.ons.census.action.model.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;
import javax.persistence.Index;
import javax.persistence.Table;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Map;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

    private int quantity;

    private UUID batchId;

}
