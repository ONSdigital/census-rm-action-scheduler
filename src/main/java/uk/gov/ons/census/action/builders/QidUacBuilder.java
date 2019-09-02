package uk.gov.ons.census.action.builders;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class QidUacBuilder {
  private static final Logger log = LoggerFactory.getLogger(QidUacBuilder.class);

  private static final Set<ActionType> initialContactActionTypes =
      Set.of(
          ActionType.ICHHQE,
          ActionType.ICHHQW,
          ActionType.ICHHQN,
          ActionType.ICL1E,
          ActionType.ICL2W,
          ActionType.ICL4N);
  private static final int NUM_OF_UAC_QID_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE = 2;
  private static final int NUM_OF_UAC_QID_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE = 1;
  private static final String WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE = "02";
  private static final String WALES_IN_WELSH_QUESTIONNAIRE_TYPE = "03";
  private static final String UNKNOWN_COUNTRY_ERROR = "Unknown Country";
  private static final String UNEXPECTED_CASE_TYPE_ERROR = "Unexpected Case Type";
  public static final String HOUSEHOLD_INITIAL_CONTACT_QUESTIONNAIRE_TREATMENT_CODE_PREFIX = "HH_Q";
  public static final String WALES_TREATMENT_CODE_SUFFIX = "W";

  private final UacQidLinkRepository uacQidLinkRepository;
  private final CaseClient caseClient;

  public QidUacBuilder(UacQidLinkRepository uacQidLinkRepository, CaseClient caseClient) {
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.caseClient = caseClient;
  }

  public UacQidTuple getUacQidLinks(Case linkedCase, ActionType actionType) {

    if (isInitialContactActionType(actionType)) {
      return fetchExistingUacQidPairsForAction(linkedCase, actionType);
    } else {
      return createNewUacQidPairsForAction(linkedCase, actionType);
    }
  }

  private UacQidTuple fetchExistingUacQidPairsForAction(Case linkedCase, ActionType actionType) {

    List<UacQidLink> uacQidLinks =
        uacQidLinkRepository.findByCaseId(linkedCase.getCaseId().toString());

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // We can't process this case with no UACs

    } else if (!actionType.equals(ActionType.ICHHQW)
        && isStateCorrectForSingleUacQidPair(linkedCase, uacQidLinks)) {
      return getUacQidTupleWithSinglePair(uacQidLinks);

    } else if (actionType.equals(ActionType.ICHHQW)
        && isStateCorrectForSecondWelshUacQidPair(linkedCase, uacQidLinks)) {
      return getUacQidTupleWithSecondWelshPair(uacQidLinks);

    } else {
      throw new RuntimeException(); // We can't process this case with the wrong number of UACs for
      // it's treatment code
    }
  }

  private boolean isStateCorrectForSingleUacQidPair(Case linkedCase, List<UacQidLink> uacQidLinks) {
    return !isQuestionnaireWelsh(linkedCase.getTreatmentCode())
        && uacQidLinks.size() == NUM_OF_UAC_QID_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE;
  }

  private boolean isStateCorrectForSecondWelshUacQidPair(
      Case linkedCase, List<UacQidLink> uacQidLinks) {
    return isQuestionnaireWelsh(linkedCase.getTreatmentCode())
        && uacQidLinks.size()
            == NUM_OF_UAC_QID_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE;
  }

  private UacQidTuple getUacQidTupleWithSinglePair(List<UacQidLink> uacQidLinks) {
    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(uacQidLinks.get(0));
    return uacQidTuple;
  }

  private UacQidTuple getUacQidTupleWithSecondWelshPair(List<UacQidLink> uacQidLinks) {
    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(
        getSpecificUacQidLinkByQuestionnaireType(
            uacQidLinks, WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE, WALES_IN_WELSH_QUESTIONNAIRE_TYPE));
    uacQidTuple.setUacQidLinkWales(
        Optional.of(
            getSpecificUacQidLinkByQuestionnaireType(
                uacQidLinks,
                WALES_IN_WELSH_QUESTIONNAIRE_TYPE,
                WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE)));
    return uacQidTuple;
  }

  private UacQidTuple createNewUacQidPairsForAction(Case linkedCase, ActionType actionType) {
    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(
        createNewUacQidPair(linkedCase, calculateQuestionnaireType(linkedCase.getTreatmentCode())));
    if (actionType.equals(ActionType.P_QU_H2)) {
      uacQidTuple.setUacQidLinkWales(
          Optional.of(createNewUacQidPair(linkedCase, WALES_IN_WELSH_QUESTIONNAIRE_TYPE)));
    }
    return uacQidTuple;
  }

  private UacQidLink createNewUacQidPair(Case linkedCase, String questionnaireType) {
    UacQidDTO newUacQidPair = caseClient.getUacQid(linkedCase.getCaseId(), questionnaireType);
    UacQidLink newUacQidLink = new UacQidLink();
    newUacQidLink.setCaseId(linkedCase.getCaseId().toString());
    newUacQidLink.setQid(newUacQidPair.getQid());
    newUacQidLink.setUac(newUacQidPair.getUac());
    // Don't persist the new UAC QID link here, that is handled by our eventual consistency model in
    // the API request
    return newUacQidLink;
  }

  private boolean isQuestionnaireWelsh(String treatmentCode) {
    return (treatmentCode.startsWith(HOUSEHOLD_INITIAL_CONTACT_QUESTIONNAIRE_TREATMENT_CODE_PREFIX)
        && treatmentCode.endsWith(WALES_TREATMENT_CODE_SUFFIX));
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

  private boolean isInitialContactActionType(ActionType actionType) {
    return initialContactActionTypes.contains(actionType);
  }

  public static String calculateQuestionnaireType(String treatmentCode) {
    String country = treatmentCode.substring(treatmentCode.length() - 1);
    if (!country.equals("E") && !country.equals("W") && !country.equals("N")) {
      log.with("treatment_code", treatmentCode).error(UNKNOWN_COUNTRY_ERROR);
      throw new IllegalArgumentException();
    }

    if (treatmentCode.startsWith("HH")) {
      switch (country) {
        case "E":
          return "01";
        case "W":
          return "02";
        case "N":
          return "04";
      }
    } else if (treatmentCode.startsWith("CI")) {
      switch (country) {
        case "E":
          return "21";
        case "W":
          return "22";
        case "N":
          return "24";
      }
    } else if (treatmentCode.startsWith("CE")) {
      switch (country) {
        case "E":
          return "31";
        case "W":
          return "32";
        case "N":
          return "34";
      }
    } else {
      log.with("treatment_code", treatmentCode).error(UNEXPECTED_CASE_TYPE_ERROR);
      throw new IllegalArgumentException();
    }

    throw new RuntimeException(); // This code should be unreachable
  }
}
