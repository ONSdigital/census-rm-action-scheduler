package uk.gov.ons.census.action.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@Component
public class FulfilmentProcessor {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleScheduler.class);
  private JdbcTemplate jdbcTemplate;
  private FulfilmentToSendRepository fulfilmentToSendRepository;

  public FulfilmentProcessor(
      JdbcTemplate jdbcTemplate, FulfilmentToSendRepository fulfilmentToSendRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.fulfilmentToSendRepository = fulfilmentToSendRepository;
  }

  @Transactional
  public void addFulfilmentBatchIdAndQuantity() {
    UUID batchId = UUID.randomUUID();
    log.with("batch_id", batchId).info("Fulfilments triggered");

    List<String> fulfilmentCodes = fulfilmentToSendRepository.findDistinctFulfilmentCode();

    fulfilmentCodes.forEach(
        fulfilmentCode -> {
          jdbcTemplate.update(
              "UPDATE actionv2.fulfilment_to_process "
                  + "SET quantity = (SELECT COUNT(*) FROM actionv2.fulfilment_to_process "
                  + "WHERE fulfilment_code = ?), batch_id = ? WHERE fulfilment_code = ?",
              fulfilmentCode,
              batchId,
              fulfilmentCode);
        });
  }
}
