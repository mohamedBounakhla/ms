package core.ms.security.DAO;

import core.ms.security.domain.MSUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MSUserDAO extends JpaRepository<MSUser, String> {
}
