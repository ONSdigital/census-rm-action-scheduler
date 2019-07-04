package uk.gov.ons.census.action.builders;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.Case;

@Component
public class PrintFileDtoBuilder {
  private final QidUacBuilder qidUacBuilder;

  public PrintFileDtoBuilder(QidUacBuilder qidUacBuilder) {
    this.qidUacBuilder = qidUacBuilder;
  }

  public PrintFileDto buildPrintFileDto(Case caze, String packCode, UUID batchUUID, String actionType) {
    UacQidTuple uacQidTuple = qidUacBuilder.getUacQidLinks(caze);

    PrintFileDto printFileDto = new PrintFileDto();
    printFileDto.setUac(uacQidTuple.getUacQidLink().getUac());
    printFileDto.setQid(uacQidTuple.getUacQidLink().getQid());

    if (uacQidTuple.getUacQidLinkWales().isPresent()) {
      printFileDto.setUacWales(uacQidTuple.getUacQidLinkWales().get().getUac());
      printFileDto.setQidWales(uacQidTuple.getUacQidLinkWales().get().getQid());
    }

    printFileDto.setCaseRef(caze.getCaseRef());

    // TODO: Don't believe we have these at the moment
    printFileDto.setTitle("");
    printFileDto.setForename("");
    printFileDto.setSurname("");

    printFileDto.setAddressLine1(caze.getAddressLine1());
    printFileDto.setAddressLine2(caze.getAddressLine2());
    printFileDto.setAddressLine3(caze.getAddressLine3());
    printFileDto.setTownName(caze.getTownName());
    printFileDto.setPostcode(caze.getPostcode());
    printFileDto.setBatchId(batchUUID.toString());
    printFileDto.setPackCode(packCode);
    printFileDto.setActionType(actionType);

    return printFileDto;
  }
}
