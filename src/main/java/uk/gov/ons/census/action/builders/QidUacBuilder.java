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

  private static final Set<String> packCodesRequiringNewUacQidPair =
      Set.of(
          ActionType.P_RL_1RL1_1.name(),
          ActionType.P_RL_1RL2B_1.name(),
          ActionType.P_RL_1RL4.name(),
          ActionType.P_RL_1RL1_2.name(),
          ActionType.P_RL_1RL2B_2.name(),
          ActionType.P_RL_2RL1_3a.name(),
          ActionType.P_RL_2RL2B_3a.name(),
          ActionType.P_QU_H1.name(),
          ActionType.P_QU_H2.name(),
          ActionType.P_QU_H4.name());
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

  public UacQidTuple getUacQidLinks(Case linkedCase, String packCode) {

    if (packCodeRequiresNewUacQids(packCode)) {
      return createUacQidTupleWithNewPairs(linkedCase, packCode);
    }

    List<UacQidLink> uacQidLinks =
        uacQidLinkRepository.findByCaseId(linkedCase.getCaseId().toString());

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // We can't process this case with UACs

    } else if (isStateCorrectForSingleUacQidPair(linkedCase, uacQidLinks)) {
      return createUacQidTupleWithSinglePair(uacQidLinks);

    } else if (isStateCorrectForSecondWelshUacQidPair(linkedCase, uacQidLinks)) {
      return createUacQidTupleWithSecondWelshPair(uacQidLinks);

    } else {
      throw new RuntimeException(); // We can't process this case with the wrong number of UACs
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

  private UacQidTuple createUacQidTupleWithSinglePair(List<UacQidLink> uacQidLinks) {
    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(uacQidLinks.get(0));
    return uacQidTuple;
  }

  private UacQidTuple createUacQidTupleWithSecondWelshPair(List<UacQidLink> uacQidLinks) {
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

  private UacQidTuple createUacQidTupleWithNewPairs(Case linkedCase, String packCode) {
    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(
        createNewUacQidPair(linkedCase, calculateQuestionnaireType(linkedCase.getTreatmentCode())));
    if (packCode.equals(ActionType.P_QU_H2.name())) {
      uacQidTuple.setUacQidLinkWales(Optional.of(createNewUacQidPair(linkedCase, 3)));
    }
    return uacQidTuple;
  }

  private UacQidLink createNewUacQidPair(Case linkedCase, int questionnaireType) {
    UacQidDTO newUacQidPair =
        caseClient.getUacQid(linkedCase.getCaseId(), Integer.toString(questionnaireType));
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

  private boolean packCodeRequiresNewUacQids(String packCode) {
    return packCodesRequiringNewUacQidPair.contains(packCode);
  }

  public static int calculateQuestionnaireType(String treatmentCode) {
    String country = treatmentCode.substring(treatmentCode.length() - 1);
    if (!country.equals("E") && !country.equals("W") && !country.equals("N")) {
      log.with("treatment_code", treatmentCode).error(UNKNOWN_COUNTRY_ERROR);
      throw new IllegalArgumentException();
    }

    if (treatmentCode.startsWith("HH")) {
      switch (country) {
        case "E":
          return 1;
        case "W":
          return 2;
        case "N":
          return 4;
      }
    } else if (treatmentCode.startsWith("CI")) {
      switch (country) {
        case "E":
          return 21;
        case "W":
          return 22;
        case "N":
          return 24;
      }
    } else if (treatmentCode.startsWith("CE")) {
      switch (country) {
        case "E":
          return 31;
        case "W":
          return 32;
        case "N":
          return 34;
      }
    } else {
      log.with("treatment_code", treatmentCode).error(UNEXPECTED_CASE_TYPE_ERROR);
      throw new IllegalArgumentException();
    }

    throw new RuntimeException(); // This code should be unreachable
  }
}
