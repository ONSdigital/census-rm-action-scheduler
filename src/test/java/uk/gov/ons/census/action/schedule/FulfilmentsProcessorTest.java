package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

public class FulfilmentsProcessorTest {
  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final FulfilmentToSendRepository fulfilmentToSendRepository =
      mock(FulfilmentToSendRepository.class);

  private final String EXPECTED_UPDATE_QUERY =
      "UPDATE actionv2.fulfilment_to_send SET quantity = (SELECT COUNT(*) "
          + "FROM actionv2.fulfilment_to_send WHERE fulfilment_code = ?), batch_id = ? "
          + "WHERE fulfilment_code = ?";

  @Test
  public void TestAddBatchIdAndQuantity() {
    // Given
    List<String> fulfilmentCodes = Collections.singletonList("P_OR_H1");
    when(fulfilmentToSendRepository.findDistinctFulfilmentCode()).thenReturn(fulfilmentCodes);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then
    fulfilmentCodes.forEach(
        fulfilmentCode -> {
          verify(jdbcTemplate)
              .update(
                  eq(EXPECTED_UPDATE_QUERY),
                  eq(fulfilmentCode),
                  any(UUID.class),
                  eq(fulfilmentCode));
        });
  }

  @Test
  public void TestwhenNoFulfilmentsAreInDatabase() {
    // Given
    when(fulfilmentToSendRepository.findDistinctFulfilmentCode())
        .thenReturn(Collections.emptyList());

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then
    verify(fulfilmentToSendRepository, times(1)).findDistinctFulfilmentCode();
    verifyNoMoreInteractions(fulfilmentToSendRepository);
  }
}
