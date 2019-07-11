package uk.gov.ons.census.action.builders;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;

@Component
public class ActionInstructionBuilder {
  private final QidUacBuilder qidUacBuilder;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public ActionInstructionBuilder(QidUacBuilder qidUacBuilder) {
    this.qidUacBuilder = qidUacBuilder;
  }

  public uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction
      buildFieldActionInstruction(Case caze, ActionRule actionRule) {

    UacQidTuple uacQidTuple = qidUacBuilder.getUacQidLinks(caze);

    uk.gov.ons.census.action.model.dto.instruction.field.ActionAddress actionAddress =
        new uk.gov.ons.census.action.model.dto.instruction.field.ActionAddress();
    actionAddress.setLine1(caze.getAddressLine1());
    actionAddress.setLine2(caze.getAddressLine2());
    actionAddress.setLine3(caze.getAddressLine3());
    actionAddress.setTownName(caze.getTownName());
    actionAddress.setPostcode(caze.getPostcode());
    actionAddress.setOrganisationName(caze.getOrganisationName());
    actionAddress.setArid(caze.getArid());
    actionAddress.setUprn(caze.getUprn());
    actionAddress.setOa(caze.getOa());
    if (caze.getLatitude() != null && !caze.getLatitude().isBlank()) {
      actionAddress.setLatitude(new BigDecimal(caze.getLatitude()));
    }
    if (caze.getLongitude() != null && !caze.getLongitude().isBlank()) {
      actionAddress.setLongitude(new BigDecimal(caze.getLongitude()));
    }

    uk.gov.ons.census.action.model.dto.instruction.field.ActionRequest actionRequest =
        new uk.gov.ons.census.action.model.dto.instruction.field.ActionRequest();
    actionRequest.setActionId(UUID.randomUUID().toString());
    actionRequest.setResponseRequired(false);
    actionRequest.setActionPlan(actionRule.getActionPlan().getId().toString());
    actionRequest.setActionType(actionRule.getActionType().toString());
    actionRequest.setAddress(actionAddress);
    actionRequest.setCaseId(caze.getCaseId().toString());
    actionRequest.setPriority(uk.gov.ons.census.action.model.dto.instruction.field.Priority.MEDIUM);
    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
    actionRequest.setIac(uacQidTuple.getUacQidLink().getUac());
    actionRequest.setAddressType(caze.getAddressType());
    actionRequest.setAddressLevel(caze.getAddressLevel());
    actionRequest.setTreatmentId(caze.getTreatmentCode());
    actionRequest.setFieldOfficerId(caze.getFieldOfficerId());
    actionRequest.setCoordinatorId(caze.getFieldCoordinatorId());
    if (caze.getCeExpectedCapacity() != null && !caze.getCeExpectedCapacity().isEmpty()) {
      actionRequest.setCeExpectedResponses(Integer.parseInt(caze.getCeExpectedCapacity()));
    }
    // TODO undeliveredAsAddress, blankQreReturned, ccsQuestionnaireUrl, ceDeliveryReqd,
    // ceCE1Complete, ceActualResponses
    uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction actionInstruction =
        new uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction();
    actionInstruction.setActionRequest(actionRequest);

    return actionInstruction;
  }
}
