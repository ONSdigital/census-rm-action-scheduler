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
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class QidUacBuilder {
  private static final Logger log = LoggerFactory.getLogger(QidUacBuilder.class);

  private static final Set<String> packCodesRequiringNewUacQidPair =
      Set.of(
          "P_RL_1RL1_1",
          "P_RL_1RL2B_1",
          "P_RL_1RL4",
          "P_RL_1RL1_2",
          "P_RL_1RL2B_2",
          "P_RL_2RL1_3a",
          "P_RL_2RL2B_3a");
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE = 2;
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE = 1;
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
    UacQidTuple uacQidTuple = new UacQidTuple();

    if (packCodeRequiresNewUacQidPair(packCode)) {
      uacQidTuple.setUacQidLink(createNewUacQidPair(linkedCase));
      return uacQidTuple;
    }

    List<UacQidLink> uacQidLinks =
        uacQidLinkRepository.findByCaseId(linkedCase.getCaseId().toString());

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // TODO: How can we process this case without a UAC?
    } else if (uacQidLinks.size() > NUM_OF_UAC_IAC_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE) {
      if (isQuestionnaireWelsh(linkedCase.getTreatmentCode())
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
    } else if (!isQuestionnaireWelsh(linkedCase.getTreatmentCode())) {
      // Implicitly from the logic above, there can only be one UAC/QID pair - the right one
      uacQidTuple.setUacQidLink(uacQidLinks.get(0));
    } else {
      // Not enough UAC/QID links for a Welsh questionnaire
      throw new RuntimeException();
    }

    return uacQidTuple;
  }

  private UacQidLink createNewUacQidPair(Case linkedCase) {
    int questionnaireType = calculateQuestionnaireType(linkedCase.getTreatmentCode());
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

  private boolean packCodeRequiresNewUacQidPair(String packCode) {
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
