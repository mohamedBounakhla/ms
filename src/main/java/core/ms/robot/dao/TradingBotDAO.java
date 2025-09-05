package core.ms.robot.dao;

import core.ms.robot.domain.TradingBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingBotDAO extends JpaRepository<TradingBot, String> {
    List<TradingBot> findByStatus(TradingBot.BotStatus status);
}