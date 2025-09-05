package core.ms.symbol.service;

import core.ms.shared.money.AssetType;
import core.ms.shared.money.Symbol;
import core.ms.symbol.domain.SymbolEntity;
import core.ms.symbol.dto.CreateSymbolRequest;
import core.ms.symbol.dto.SymbolDTO;
import core.ms.symbol.dto.UpdateSymbolRequest;
import core.ms.symbol.dao.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SymbolService {

    private static final Logger logger = LoggerFactory.getLogger(SymbolService.class);

    @Autowired
    private SymbolRepository symbolRepository;

    @Cacheable(value = "symbols", key = "'all'")
    public List<Symbol> getAllActiveSymbols() {
        return symbolRepository.findByActiveTrueOrderByCodeAsc()
                .stream()
                .map(SymbolEntity::toDomainSymbol)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "symbols", key = "#code.toUpperCase()")
    public Optional<Symbol> findByCode(String code) {
        return symbolRepository.findActiveByCode(code)
                .map(SymbolEntity::toDomainSymbol);
    }

    public Symbol getByCodeOrThrow(String code) {
        return findByCode(code)
                .orElseThrow(() -> new SymbolNotFoundException("Symbol not found: " + code));
    }

    public List<Symbol> getSymbolsByType(AssetType type) {
        return symbolRepository.findByAssetTypeAndActiveTrue(type)
                .stream()
                .map(SymbolEntity::toDomainSymbol)
                .collect(Collectors.toList());
    }

    public List<SymbolDTO> getAllSymbolDTOs() {
        return symbolRepository.findByActiveTrueOrderByCodeAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<SymbolDTO> getSymbolDTO(String code) {
        return symbolRepository.findActiveByCode(code)
                .map(this::toDTO);
    }

    @CacheEvict(value = "symbols", allEntries = true)
    public SymbolDTO createSymbol(CreateSymbolRequest request) {
        if (symbolRepository.existsById(request.getCode().toUpperCase())) {
            throw new SymbolAlreadyExistsException("Symbol already exists: " + request.getCode());
        }

        SymbolEntity entity = new SymbolEntity(
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.getAssetType(),
                request.getBaseCurrency(),
                request.getQuoteCurrency(),
                request.getMinOrderSize(),
                request.getMaxOrderSize(),
                request.getTickSize(),
                request.getLotSize()
        );

        entity = symbolRepository.save(entity);
        logger.info("Created new symbol: {}", entity.getCode());

        return toDTO(entity);
    }

    @CacheEvict(value = "symbols", allEntries = true)
    public SymbolDTO updateSymbol(String code, UpdateSymbolRequest request) {
        SymbolEntity entity = symbolRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new SymbolNotFoundException("Symbol not found: " + code));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getMinOrderSize() != null) {
            entity.setMinOrderSize(request.getMinOrderSize());
        }
        if (request.getMaxOrderSize() != null) {
            entity.setMaxOrderSize(request.getMaxOrderSize());
        }
        if (request.getTickSize() != null) {
            entity.setTickSize(request.getTickSize());
        }
        if (request.getLotSize() != null) {
            entity.setLotSize(request.getLotSize());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        entity = symbolRepository.save(entity);
        logger.info("Updated symbol: {}", entity.getCode());

        return toDTO(entity);
    }

    @CacheEvict(value = "symbols", allEntries = true)
    public void deactivateSymbol(String code) {
        SymbolEntity entity = symbolRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new SymbolNotFoundException("Symbol not found: " + code));

        entity.setActive(false);
        symbolRepository.save(entity);
        logger.info("Deactivated symbol: {}", code);
    }

    public boolean isSymbolActive(String code) {
        return symbolRepository.existsByCodeAndActiveTrue(code.toUpperCase());
    }

    private SymbolDTO toDTO(SymbolEntity entity) {
        return new SymbolDTO(
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getAssetType(),
                entity.getBaseCurrency(),
                entity.getQuoteCurrency(),
                entity.isActive(),
                entity.getMinOrderSize(),
                entity.getMaxOrderSize(),
                entity.getTickSize(),
                entity.getLotSize()
        );
    }

    // Exceptions
    public static class SymbolNotFoundException extends RuntimeException {
        public SymbolNotFoundException(String message) {
            super(message);
        }
    }

    public static class SymbolAlreadyExistsException extends RuntimeException {
        public SymbolAlreadyExistsException(String message) {
            super(message);
        }
    }
}