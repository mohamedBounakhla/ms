package core.ms.security.dao;

import core.ms.security.domain.MSUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MSUserDAO extends JpaRepository<MSUser, String> {
}
