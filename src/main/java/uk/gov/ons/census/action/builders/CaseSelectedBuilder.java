package uk.gov.ons.census.action.builders;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.Event;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.FieldCaseSelected;
import uk.gov.ons.census.action.model.dto.Payload;
import uk.gov.ons.census.action.model.dto.PrintCaseSelected;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;

@Component
public class CaseSelectedBuilder {
  public ResponseManagementEvent buildPrintMessage(PrintFileDto printFileDto, String actionRuleId) {
    ResponseManagementEvent responseManagementEvent =
        buildEventWithoutPayload(EventType.PRINT_CASE_SELECTED);
    PrintCaseSelected printCaseSelected = new PrintCaseSelected();
    responseManagementEvent.getPayload().setPrintCaseSelected(printCaseSelected);

    printCaseSelected.setActionRuleId(actionRuleId);
    printCaseSelected.setBatchId(printFileDto.getBatchId());
    printCaseSelected.setCaseRef(printFileDto.getCaseRef());
    printCaseSelected.setPackCode(printFileDto.getPackCode());

    return responseManagementEvent;
  }

  public ResponseManagementEvent buildFieldMessage(String caseRef, UUID actionRuleId) {
    ResponseManagementEvent responseManagementEvent =
        buildEventWithoutPayload(EventType.FIELD_CASE_SELECTED);
    FieldCaseSelected fieldCaseSelected = new FieldCaseSelected();
    responseManagementEvent.getPayload().setFieldCaseSelected(fieldCaseSelected);

    fieldCaseSelected.setActionRuleId(actionRuleId.toString());
    fieldCaseSelected.setCaseRef(Integer.parseInt(caseRef));

    return responseManagementEvent;
  }

  private ResponseManagementEvent buildEventWithoutPayload(EventType eventType) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Event event = new Event();
    responseManagementEvent.setEvent(event);
    Payload payload = new Payload();
    responseManagementEvent.setPayload(payload);

    event.setType(eventType);
    event.setSource("ACTION_SCHEDULER");
    event.setChannel("RM");
    event.setDateTime(DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now(ZoneId.of("UTC"))));
    event.setTransactionId(UUID.randomUUID().toString());

    return responseManagementEvent;
  }
}
