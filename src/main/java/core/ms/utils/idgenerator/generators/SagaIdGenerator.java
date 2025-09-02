package core.ms.utils.idgenerator.generators;

import core.ms.utils.idgenerator.BaseIdGenerator;
import core.ms.utils.idgenerator.IdGeneratorBean;

@IdGeneratorBean("saga")
public class SagaIdGenerator extends BaseIdGenerator {
    @Override
    protected String getPrefix() {
        return "SAGA-";
    }
}