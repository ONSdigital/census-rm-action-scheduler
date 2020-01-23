package uk.gov.ons.census.action.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.entity.FulfilmentCodeCount;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@Component
public class FulfilmentProcessor {

  private JdbcTemplate jdbcTemplate;
  private FulfilmentToSendRepository fulfilmentToSendRepository;

  public FulfilmentProcessor(
      JdbcTemplate jdbcTemplate, FulfilmentToSendRepository fulfilmentToSendRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.fulfilmentToSendRepository = fulfilmentToSendRepository;
  }

  public void addFulfilmentBatchIdAndQuantity() {
    List<FulfilmentCodeCount> fulfilmentsToSend =
        fulfilmentToSendRepository.findCountOfFulfilments();

    fulfilmentsToSend.forEach(
        (fulfilment) -> {
          jdbcTemplate.update(
              "UPDATE actionv2.fulfilment_to_send SET quantity = ?, batch_id = ? "
                  + "where fulfilment_code = ? ",
              fulfilment.getCount(),
              UUID.randomUUID(),
              fulfilment.getFulfilmentCode());
        });
  }
}
