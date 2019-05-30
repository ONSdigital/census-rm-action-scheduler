package uk.gov.ons.census.action.model.dto.instruction.field;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "ActionPause",
    propOrder = {"effectiveDate", "code", "reason", "holdUntil"})
@Data
public class ActionPause {

  protected Date effectiveDate;
  protected String code;
  protected String reason;
  protected Date holdUntil;
}
