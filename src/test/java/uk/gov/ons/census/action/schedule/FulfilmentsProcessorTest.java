package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.model.entity.FulfilmentCodeCount;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

public class FulfilmentsProcessorTest {
  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final FulfilmentToSendRepository fulfilmentToSendRepository =
      mock(FulfilmentToSendRepository.class);

  private final String EXPECTED_UPDATE_QUERY =
      "UPDATE actionv2.fulfilment_to_send SET quantity = ?, batch_id = ? where fulfilment_code = ? ";

  @Test
  public void TestAddBatchIdAndQuantity() {

    List<FulfilmentCodeCount> fulfilmentCodeCounts = createListOfFulfilments();

    when(fulfilmentToSendRepository.findCountOfFulfilments()).thenReturn(fulfilmentCodeCounts);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then
    fulfilmentCodeCounts.forEach(
        fulfilmentCodeCount -> {
          verify(jdbcTemplate, times(2))
              .update(
                  eq(EXPECTED_UPDATE_QUERY),
                  eq(fulfilmentCodeCount.getCount()),
                  any(UUID.class),
                  eq(fulfilmentCodeCount.getFulfilmentCode()));
        });
  }

  private List<FulfilmentCodeCount> createListOfFulfilments() {
    List<FulfilmentCodeCount> fulfilmentsToSend = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      FulfilmentCodeCount fulfilmentCodeCount = new FulfilmentCodeCount("P_OR_H1", 2L);
      fulfilmentsToSend.add(fulfilmentCodeCount);
    }

    return fulfilmentsToSend;
  }

  @Test
  public void TestwhenNoFulfilmentsAreInDatabase() {

    List<FulfilmentCodeCount> emptyFulfilmentsList = new ArrayList<>();
    when(fulfilmentToSendRepository.findCountOfFulfilments()).thenReturn(emptyFulfilmentsList);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();
    // Then

    verify(fulfilmentToSendRepository, times(1)).findCountOfFulfilments();
    verifyNoMoreInteractions(fulfilmentToSendRepository);
  }
}
