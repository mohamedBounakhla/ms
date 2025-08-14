package core.ms.utils.idgenerator.generators;

import core.ms.utils.idgenerator.BaseIdGenerator;
import core.ms.utils.idgenerator.IdGeneratorBean;

@IdGeneratorBean("cash-reservation")
public class CashReservationIdGenerator extends BaseIdGenerator {
    @Override
    protected String getPrefix() {
        return "CASH-RES-";
    }
}