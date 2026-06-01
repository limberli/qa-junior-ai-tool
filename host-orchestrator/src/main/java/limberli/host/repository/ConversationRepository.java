package limberli.host.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findTop10ByOrderByCreatedAtDesc();

    Page<Conversation> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);
}
