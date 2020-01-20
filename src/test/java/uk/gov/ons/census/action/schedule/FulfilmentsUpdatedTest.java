package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import uk.gov.ons.census.action.model.dto.FulfilmentMapperDTO;
import uk.gov.ons.census.action.utility.FulfilmentMapper;

public class FulfilmentsUpdatedTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    public void TestAddBatchIdAndQuantity() {
        FulfilmentMapperDTO fulfilmentMapperDTO = new FulfilmentMapperDTO();
        fulfilmentMapperDTO.setCount(1);
        fulfilmentMapperDTO.setFulfilmentCode("P_OR_H1");
        List<FulfilmentMapperDTO> fulfilmentsToSend = new ArrayList<>();
        fulfilmentsToSend.add(fulfilmentMapperDTO);

        when(jdbcTemplate.query(any(String.class), any(FulfilmentMapper.class)))
                .thenReturn(fulfilmentsToSend);

        // When
        FulfilmentUpdater fulfilmentUpdater = new FulfilmentUpdater(jdbcTemplate);
        fulfilmentUpdater.addFulfilmentBatchIdAndQuantity();

        // Then
        verify(jdbcTemplate)
                .update(
                        any(String.class),
                        eq(fulfilmentMapperDTO.getCount()),
                        any(UUID.class),
                        eq(fulfilmentMapperDTO.getFulfilmentCode()));
    }

    @Test
    public void TestNoFulfilmentsInDatabase() {

        // When
        FulfilmentUpdater fulfilmentUpdater = new FulfilmentUpdater(jdbcTemplate);
        fulfilmentUpdater.addFulfilmentBatchIdAndQuantity();

        // Then
        verify(jdbcTemplate, never()).update((PreparedStatementCreator) any(), any());
    }
}
