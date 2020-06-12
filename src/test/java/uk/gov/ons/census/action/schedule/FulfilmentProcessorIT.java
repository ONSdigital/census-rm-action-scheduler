package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.FulfilmentToProcess;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.CaseToProcessRepository;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentProcessorIT {

  @Autowired private FulfilmentToSendRepository fulfilmentToSendRepository;
  @Autowired private CaseToProcessRepository caseToProcessRepository;
  @Autowired private FulfilmentProcessor fulfilmentProcessor;
  @Autowired private CaseRepository caseRepository;

  private static final Map<String, Integer> fulfilmentQuantity =
      new HashMap<>() {
        {
          put("P_OR_H1", 2);
          put("P_OR_H2", 2);
          put("P_OR_H2W", 3);
          put("P_OR_H4", 4);
        }
      };

  @Before
  @Transactional
  public void setUp() {
    fulfilmentToSendRepository.deleteAll();
    caseToProcessRepository.deleteAllInBatch();
  }

  @Test
  public void TestAddingBatchIdAndQuantity() {
    // Given

    fulfilmentQuantity.forEach(this::createMultipleFulfilments);

    // When
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then

    List<FulfilmentToProcess> fulfilmentsToSend = fulfilmentToSendRepository.findAll();
    fulfilmentsToSend.forEach((this::AssertQuantityandBatchIdAreCorrect));
  }

  private void AssertQuantityandBatchIdAreCorrect(FulfilmentToProcess fulfilmentToProcess) {
    fulfilmentQuantity.forEach(
        (fulfilment, quantity) -> {
          if (fulfilment.equals(fulfilmentToProcess.getFulfilmentCode())) {
            assertThat(fulfilmentToProcess.getQuantity()).isEqualTo(quantity);
            assertThat(fulfilmentToProcess.getBatchId()).isNotNull();
          }
        });
  }

  private void createMultipleFulfilments(String fulfilment_code, Integer quantity) {
    for (int i = 0; i < quantity; i++) {
      saveFulfilmentToTable(fulfilment_code);
    }
  }

  private void saveFulfilmentToTable(String fulfilment_code) {
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);
    caze = caseRepository.saveAndFlush(caze);

    FulfilmentToProcess fulfilmentsBeforeQuantityAndBatchId =
        easyRandom.nextObject(FulfilmentToProcess.class);
    fulfilmentsBeforeQuantityAndBatchId.setCaze(caze);
    fulfilmentsBeforeQuantityAndBatchId.setQuantity(null);
    fulfilmentsBeforeQuantityAndBatchId.setBatchId(null);

    fulfilmentsBeforeQuantityAndBatchId.setFulfilmentCode(fulfilment_code);

    fulfilmentToSendRepository.saveAndFlush(fulfilmentsBeforeQuantityAndBatchId);
  }
}
