package uk.gov.ons.census.action.builders;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;

@Component
public class PrintFileDtoBuilder {
  private final QidUacBuilder qidUacBuilder;

  public PrintFileDtoBuilder(QidUacBuilder qidUacBuilder) {
    this.qidUacBuilder = qidUacBuilder;
  }

  public PrintFileDto buildPrintFileDto(
      Case selectedCase, String packCode, UUID batchUUID, ActionType actionType) {

    UacQidTuple uacQidTuple = qidUacBuilder.getUacQidLinks(selectedCase, actionType);

    PrintFileDto printFileDto = new PrintFileDto();
    printFileDto.setUac(uacQidTuple.getUacQidLink().getUac());
    printFileDto.setQid(uacQidTuple.getUacQidLink().getQid());

    if (uacQidTuple.getUacQidLinkWales().isPresent()) {
      printFileDto.setUacWales(uacQidTuple.getUacQidLinkWales().get().getUac());
      printFileDto.setQidWales(uacQidTuple.getUacQidLinkWales().get().getQid());
    }

    printFileDto.setCaseRef(selectedCase.getCaseRef());
    printFileDto.setAddressLine1(selectedCase.getAddressLine1());
    printFileDto.setAddressLine2(selectedCase.getAddressLine2());
    printFileDto.setAddressLine3(selectedCase.getAddressLine3());
    printFileDto.setTownName(selectedCase.getTownName());
    printFileDto.setPostcode(selectedCase.getPostcode());
    printFileDto.setBatchId(batchUUID.toString());
    printFileDto.setPackCode(packCode);
    printFileDto.setActionType(actionType.toString());
    printFileDto.setFieldCoordinatorId(selectedCase.getFieldCoordinatorId());

    return printFileDto;
  }
}
