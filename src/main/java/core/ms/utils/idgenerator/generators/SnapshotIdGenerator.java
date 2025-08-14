package core.ms.utils.idgenerator.generators;

import core.ms.utils.idgenerator.BaseIdGenerator;
import core.ms.utils.idgenerator.IdGeneratorBean;

@IdGeneratorBean
public class SnapshotIdGenerator extends BaseIdGenerator {
    @Override
    protected String getPrefix() {
        return "SNAPSHOT-";
    }
}