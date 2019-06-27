package uk.gov.ons.census.action.builders;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class QidUacBuilder {
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_BY_A_WALES_INITIAL_CONTACT_QUESTIONNAIRE = 2;
  private static final int NUM_OF_UAC_IAC_PAIRS_NEEDED_FOR_SINGLE_LANGUAGE = 1;
  private static final String WALES_IN_ENGLISH_QUESTIONNAIRE_TYPE = "02";
  private static final String WALES_IN_WELSH_QUESTIONNAIRE_TYPE = "03";
  private static final String HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX = "HH_Q";
  private static final String WALES_TREATMENT_CODE_SUFFIX = "W";

  private final UacQidLinkRepository uacQidLinkRepository;

  public QidUacBuilder(UacQidLinkRepository uacQidLinkRepository) {
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public UacQidTuple getUacQidLinks(Case caze) {
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

  private boolean isQuestionnaireWelsh(String treatmentCode) {
    return (treatmentCode.startsWith(HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX)
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
}
