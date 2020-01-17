package uk.gov.ons.census.action.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.FulfilmentMapperDTO;
import uk.gov.ons.census.action.model.entity.FulfilmentsToSend;
import uk.gov.ons.census.action.model.repository.FulfilmentsToSendRepository;

@Component
public class FulfilmentUpdater {

  private JdbcTemplate jdbcTemplate;
  private FulfilmentsToSendRepository fulfilmentsToSendRepository;

  public FulfilmentUpdater(JdbcTemplate jdbcTemplate, FulfilmentsToSendRepository fulfilmentsToSendRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.fulfilmentsToSendRepository = fulfilmentsToSendRepository;
  }

  public void addFulfilmentBatchIdAndQuantity() {
    String query =
        "select fulfilment_code, count(*) from actionv2.fulfilments_to_send group by fulfilment_code";
    List<FulfilmentMapperDTO> fulfilmentsToSend =
        jdbcTemplate.query(query, new uk.gov.ons.census.action.utility.FulfilmentMapper());

    fulfilmentsToSend.forEach(
        (fulfilment) -> {
          jdbcTemplate.update(
              "UPDATE actionv2.fulfilments_to_send SET quantity = ?, batch_id = ? "
                  + "where fulfilment_code = ? ",
              fulfilment.getCount(),
              UUID.randomUUID(),
              fulfilment.getFulfilment_code());
        });
  }
}
