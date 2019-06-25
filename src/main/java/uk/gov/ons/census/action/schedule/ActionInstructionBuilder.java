package uk.gov.ons.census.action.schedule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.printer.Priority;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class ActionInstructionBuilder {
  private static final String HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX = "HH_Q";
  private static final String WALES_TREATMENT_CODE_SUFFIX = "W";
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE = 2;
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE = 1;
  private static final String WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE = "02";
  private static final String WALES_IN_WELSH_QUESTIONNAIRE_TYPE = "03";

  private final UacQidLinkRepository uacQidLinkRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public ActionInstructionBuilder(UacQidLinkRepository uacQidLinkRepository) {
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public ActionInstruction buildPrinterActionInstruction(Case caze, ActionRule actionRule) {

    UacQidTuple uacQidTuple = getUacQidLinks(caze);

    ActionEvent actionEvent = new ActionEvent();
    // Legacy hard-coded value to satisfy Action Exporter which should be refactored
    actionEvent
        .getEvents()
        .add("CASE_CREATED : null : SYSTEM : Case created when Initial creation of case");

    ActionAddress actionAddress = new ActionAddress();
    actionAddress.setLine1(caze.getAddressLine1());
    actionAddress.setLine2(caze.getAddressLine2());
    actionAddress.setLine3(caze.getAddressLine3());
    actionAddress.setTownName(caze.getTownName());
    actionAddress.setPostcode(caze.getPostcode());
    actionAddress.setOrganisationName(caze.getOrganisationName());
    ActionRequest actionRequest = new ActionRequest();
    actionRequest.setActionId(UUID.randomUUID().toString());
    actionRequest.setResponseRequired(false);
    actionRequest.setActionPlan(actionRule.getActionPlan().getId().toString());

    actionRequest.setActionType(actionRule.getActionType().toString());

    actionRequest.setAddress(actionAddress);

    // Hardcoded because this is irrelevant for Census - to be refactored away later
    actionRequest.setLegalBasis("Statistics of Trade Act 1947");

    // Hardcoded because this isn't needed by Action Exporter - will be removed later
    actionRequest.setCaseGroupStatus("NOTSTARTED");

    actionRequest.setCaseId(caze.getCaseId().toString());

    actionRequest.setPriority(Priority.MEDIUM);
    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
    actionRequest.setIac(uacQidTuple.getUacQidLink().getUac());
    actionRequest.setQid(uacQidTuple.getUacQidLink().getQid());

    if (uacQidTuple.getUacQidLinkWales().isPresent()) {
      actionRequest.setIacWales(uacQidTuple.getUacQidLinkWales().get().getUac());
      actionRequest.setQidWales(uacQidTuple.getUacQidLinkWales().get().getQid());
    }

    actionRequest.setEvents(actionEvent);

    // All hard-coded for legacy reasons - Action Exporter should not need any of these values
    actionRequest.setExerciseRef("201904");
    actionRequest.setUserDescription("Census-FNSM580JQE3M4");
    actionRequest.setSurveyName("Census-FNSM580JQE3M4");
    actionRequest.setSurveyRef("Census-FNSM580JQE3M4");
    actionRequest.setReturnByDate("27/04");
    actionRequest.setSampleUnitRef("DDR190314000000516472");

    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionRequest(actionRequest);

    return actionInstruction;
  }

  public uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction
      buildFieldActionInstruction(Case caze, ActionRule actionRule) {

    UacQidTuple uacQidTuple = getUacQidLinks(caze);

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

  private boolean isQuestionnaireWelsh(String treatmentCode) {
    return (treatmentCode.startsWith(HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX)
        && treatmentCode.endsWith(WALES_TREATMENT_CODE_SUFFIX));
  }

  private UacQidTuple getUacQidLinks(Case caze) {
    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findByCaseId(caze.getCaseId().toString());
    UacQidTuple uacQidTuple = new UacQidTuple();

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // TODO: How can we process this case without a UAC?
    } else if (uacQidLinks.size() > NUM_OF_UAC_IAC_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE) {
      if (isQuestionnaireWelsh(caze.getTreatmentCode())
          && uacQidLinks.size()
              == NUM_OF_UAC_IAC_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE) {
        uacQidTuple.setUacQidLink(
            getSpecificUacQidLinkByQuestionnaireType(
                uacQidLinks,
                WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE,
                WALES_IN_WELSH_QUESTIONNAIRE_TYPE));
        uacQidTuple.setUacQidLinkWales(
            Optional.ofNullable(
                getSpecificUacQidLinkByQuestionnaireType(
                    uacQidLinks,
                    WALES_IN_WELSH_QUESTIONNAIRE_TYPE,
                    WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE)));
      } else {
        throw new RuntimeException(); // TODO: How do we know which one to use?
      }
    } else if (!isQuestionnaireWelsh(caze.getTreatmentCode())) {
      // Implicitly from the logic above, there can only be one UAC/QID pair - the right one
      uacQidTuple.setUacQidLink(uacQidLinks.get(0));
    } else {
      // Not enough UAC/QID links for a Welsh questionnaire
      throw new RuntimeException();
    }

    return uacQidTuple;
  }

  private UacQidLink getSpecificUacQidLinkByQuestionnaireType(
      List<UacQidLink> uacQidLinks,
      String wantedQuestionnaireType,
      String otherAllowableQuestionnaireType) {
    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.getQid().startsWith(wantedQuestionnaireType)) {
        return uacQidLink;
      } else if (!uacQidLink.getQid().startsWith(otherAllowableQuestionnaireType)) {
        // This shouldn't happen - why have we got non-allowable type on this case?
        throw new RuntimeException();
      }
    }

    throw new RuntimeException(); // We can't find the one we wanted
  }

  @Data
  private class UacQidTuple {
    private UacQidLink uacQidLink;
    private Optional<UacQidLink> uacQidLinkWales = Optional.ofNullable(null);
  }
}
