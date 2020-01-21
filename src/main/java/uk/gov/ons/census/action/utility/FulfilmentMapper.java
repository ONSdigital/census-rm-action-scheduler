package uk.gov.ons.census.action.utility;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class FulfilmentMapper
    implements RowMapper<uk.gov.ons.census.action.model.entity.FulfilmentMapper> {

  @Override
  public uk.gov.ons.census.action.model.entity.FulfilmentMapper mapRow(ResultSet rs, int rowNum)
      throws SQLException {
    uk.gov.ons.census.action.model.entity.FulfilmentMapper fulfilmentMapper =
        new uk.gov.ons.census.action.model.entity.FulfilmentMapper();

    fulfilmentMapper.setFulfilmentCode(rs.getString("fulfilment_code"));
    fulfilmentMapper.setCount(rs.getInt("count"));

    return fulfilmentMapper;
  }
}
