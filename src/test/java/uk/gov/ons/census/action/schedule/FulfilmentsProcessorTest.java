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
    FulfilmentCodeCount fulfilmentCodeCount = new FulfilmentCodeCount("P_OR_H1", 1L);
    List<FulfilmentCodeCount> fulfilmentsToSend = new ArrayList<>();
    fulfilmentsToSend.add(fulfilmentCodeCount);

    when(fulfilmentToSendRepository.findCountOfFulfilments()).thenReturn(fulfilmentsToSend);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then

    verify(jdbcTemplate, times(1))
        .update(
            eq(EXPECTED_UPDATE_QUERY),
            eq(fulfilmentCodeCount.getCount()),
            any(UUID.class),
            eq(fulfilmentCodeCount.getFulfilmentCode()));
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
