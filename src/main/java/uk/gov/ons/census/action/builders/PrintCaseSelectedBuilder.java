package uk.gov.ons.census.action.builders;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.Event;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.Payload;
import uk.gov.ons.census.action.model.dto.PrintCaseSelected;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;

@Component
public class PrintCaseSelectedBuilder {
  public ResponseManagementEvent buildMessage(PrintFileDto printFileDto, UUID actionRuleId) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Event event = new Event();
    responseManagementEvent.setEvent(event);
    Payload payload = new Payload();
    responseManagementEvent.setPayload(payload);
    PrintCaseSelected printCaseSelected = new PrintCaseSelected();
    payload.setPrintCaseSelected(printCaseSelected);

    event.setType(EventType.PRINT_CASE_SELECTED);
    event.setSource("ACTION_SCHEDULER");
    event.setChannel("RM");
    event.setDateTime(DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now(ZoneId.of("UTC"))));
    event.setTransactionId(UUID.randomUUID().toString());

    printCaseSelected.setActionRuleId(actionRuleId.toString());
    printCaseSelected.setBatchId(printFileDto.getBatchId());
    printCaseSelected.setCaseRef(printFileDto.getCaseRef());
    printCaseSelected.setPackCode(printFileDto.getPackCode());

    return responseManagementEvent;
  }
}
