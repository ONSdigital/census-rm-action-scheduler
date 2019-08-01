package uk.gov.ons.census.action.builders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;

@Component
public class FieldworkFollowupBuilder {

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public FieldworkFollowup buildFieldworkFollowup(Case caze, ActionRule actionRule) {

    FieldworkFollowup followup = new FieldworkFollowup();
    followup.setAddressLine1(caze.getAddressLine1());
    followup.setAddressLine2(caze.getAddressLine2());
    followup.setAddressLine3(caze.getAddressLine3());
    followup.setTownName(caze.getTownName());
    followup.setPostcode(caze.getPostcode());
    followup.setEstabType(caze.getEstabType());
    followup.setOrganisationName(caze.getOrganisationName());
    followup.setArid(caze.getArid());
    followup.setUprn(caze.getUprn());
    followup.setOa(caze.getOa());
    followup.setArid(caze.getArid());
    followup.setLatitude(caze.getLatitude());
    followup.setLongitude(caze.getLongitude());
    followup.setActionPlan(actionRule.getActionPlan().getId().toString());
    followup.setActionType(actionRule.getActionType().toString());
    followup.setCaseId(caze.getCaseId().toString());
    followup.setCaseRef(Integer.toString(caze.getCaseRef()));
    followup.setAddressType(caze.getAddressType());
    followup.setAddressLevel(caze.getAddressLevel());
    followup.setTreatmentCode(caze.getTreatmentCode());
    followup.setFieldOfficerId(caze.getFieldOfficerId());
    followup.setFieldCoordinatorId(caze.getFieldCoordinatorId());
    followup.setCeExpectedCapacity(caze.getCeExpectedCapacity());

    // TODO: set surveyName, undeliveredAsAddress and blankQreReturned from caze
    followup.setSurveyName("CENSUS");
    followup.setUndeliveredAsAddress(false);
    followup.setBlankQreReturned(false);

    // TODO: ccsQuestionnaireUrl, ceDeliveryReqd,
    // ceCE1Complete, ceActualResponses

    return followup;
  }
}
