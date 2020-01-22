package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.FulfilmentToSend;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentProcessorIT {

  @Autowired FulfilmentToSendRepository fulfilmentToSendRepository;
  @Autowired FulfilmentProcessor fulfilmentProcessor;
  @Autowired CaseRepository caseRepository;

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
  }

  @Test
  public void TestAddingBatchIdAndQuantity() {
    // Given

    fulfilmentQuantity.forEach(this::createMultipleFulfilments);

    // When
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then

    List<FulfilmentToSend> fulfilmentsToSend = fulfilmentToSendRepository.findAll();
    fulfilmentsToSend.forEach((this::AssertQuantityandBatchIdAreCorrect));
  }

  private void AssertQuantityandBatchIdAreCorrect(FulfilmentToSend fulfilmentToSend) {
    fulfilmentQuantity.forEach(
        (fulfilment, quantity) -> {
          if (fulfilment.equals(fulfilmentToSend.getFulfilmentCode())) {
            assertThat(fulfilmentToSend.getQuantity()).isEqualTo(quantity);
            assertThat(fulfilmentToSend.getBatchId()).isNotNull();
          }
        });
  }

  private void createMultipleFulfilments(String fulfilment_code, Integer quantity) {
    for (int i = 0; i < quantity; i++) {
      loopThroughHashMap(fulfilment_code);
    }
  }

  private void loopThroughHashMap(String fulfilment_code) {
    EasyRandom easyRandom = new EasyRandom();
    PrintFileDto printFileDto = easyRandom.nextObject(PrintFileDto.class);
    FulfilmentToSend fulfilmentsBeforeQuantityAndBatchId = new FulfilmentToSend();

    fulfilmentsBeforeQuantityAndBatchId.setFulfilmentCode(fulfilment_code);
    fulfilmentsBeforeQuantityAndBatchId.setMessageData(printFileDto);

    fulfilmentToSendRepository.saveAndFlush(fulfilmentsBeforeQuantityAndBatchId);
  }
}
