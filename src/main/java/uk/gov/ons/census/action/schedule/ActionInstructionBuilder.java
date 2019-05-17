package uk.gov.ons.census.action.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.instruction.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.Priority;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class ActionInstructionBuilder {
  private final UacQidLinkRepository uacQidLinkRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public ActionInstructionBuilder(UacQidLinkRepository uacQidLinkRepository) {
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public ActionInstruction buildActionInstruction(Case caze, ActionRule actionRule) {

    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findByCaseId(caze.getCaseId().toString());

    UacQidLink uacQidLink;
    Optional<UacQidLink> uacQidLinkWales = Optional.ofNullable(null);

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // TODO: How can we process this case without a UAC?
    } else if (uacQidLinks.size() > 1) {
      if (isQuestionnaireWelsh(caze.getTreatmentCode()) && uacQidLinks.size() == 2) {
        uacQidLink = getSpecificUacQidLinkByQuestionnaireType(uacQidLinks, "02", "03");
        uacQidLinkWales =
            Optional.ofNullable(getSpecificUacQidLinkByQuestionnaireType(uacQidLinks, "03", "02"));
      } else {
        throw new RuntimeException(); // TODO: How do we know which one to use?
      }
    } else if (!isQuestionnaireWelsh(caze.getTreatmentCode())) {
      uacQidLink = uacQidLinks.get(0);
    } else {
      // Not enough UAC/QID links for a Welsh questionnaire
      throw new RuntimeException();
    }

    ActionEvent actionEvent = new ActionEvent();
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
    actionRequest.setLegalBasis("Statistics of Trade Act 1947");
    actionRequest.setCaseGroupStatus("NOTSTARTED");
    actionRequest.setCaseId(caze.getCaseId().toString());

    actionRequest.setPriority(Priority.MEDIUM);
    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
    actionRequest.setIac(uacQidLink.getUac());
    actionRequest.setQid(uacQidLink.getQid());

    if (uacQidLinkWales.isPresent()) {
      actionRequest.setIacWales(uacQidLinkWales.get().getUac());
      actionRequest.setQidWales(uacQidLinkWales.get().getQid());
    }

    actionRequest.setEvents(actionEvent);
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

  private boolean isQuestionnaireWelsh(String treatmentCode) {
    return (treatmentCode.startsWith("HH_Q") && treatmentCode.endsWith("W"));
  }

  private UacQidLink getSpecificUacQidLinkByQuestionnaireType(
      List<UacQidLink> uacQidLinks,
      String wantedQuestionnaireType,
      String otherAllowableQuestionnaireType) {
    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.getQid().startsWith(wantedQuestionnaireType)) {
        return uacQidLink;
      } else if (!uacQidLink.getQid().startsWith(otherAllowableQuestionnaireType)) {
        // This shouldn't happen - why have we got non allowable type on this case?
        throw new RuntimeException();
      }
    }

    throw new RuntimeException(); // We can't find the one we wanted
  }
}
