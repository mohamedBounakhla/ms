package core.ms.utils.idgenerator.generators;

import core.ms.utils.idgenerator.BaseIdGenerator;
import core.ms.utils.idgenerator.IdGeneratorBean;

@IdGeneratorBean("asset-reservation")
public class AssetReservationIdGenerator extends BaseIdGenerator {
    @Override
    protected String getPrefix() {
        return "ASSET-RES-";
    }
}
