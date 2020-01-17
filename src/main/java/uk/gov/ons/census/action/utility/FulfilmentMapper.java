package uk.gov.ons.census.action.utility;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import uk.gov.ons.census.action.model.dto.FulfilmentMapperDTO;

public class FulfilmentMapper implements RowMapper<FulfilmentMapperDTO> {

  @Override
  public FulfilmentMapperDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
    FulfilmentMapperDTO fulfilmentMapper = new FulfilmentMapperDTO();

    fulfilmentMapper.setFulfilment_code(rs.getString("fulfilment_code"));
    fulfilmentMapper.setCount(rs.getInt("count"));

    return fulfilmentMapper;
  }
}
