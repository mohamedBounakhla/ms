package core.ms.symbol.dao;

import core.ms.shared.money.AssetType;
import core.ms.symbol.domain.SymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymbolRepository extends JpaRepository<SymbolEntity, String> {

    List<SymbolEntity> findByActiveTrue();

    List<SymbolEntity> findByActiveTrueOrderByCodeAsc();

    List<SymbolEntity> findByAssetTypeAndActiveTrue(AssetType assetType);

    @Query("SELECT s FROM SymbolEntity s WHERE s.active = true AND s.assetType = :type ORDER BY s.code")
    List<SymbolEntity> findActiveByType(@Param("type") AssetType type);

    @Query("SELECT s FROM SymbolEntity s WHERE UPPER(s.code) = UPPER(:code) AND s.active = true")
    Optional<SymbolEntity> findActiveByCode(@Param("code") String code);

    boolean existsByCodeAndActiveTrue(String code);

    @Query("SELECT COUNT(s) FROM SymbolEntity s WHERE s.active = true")
    long countActive();
}