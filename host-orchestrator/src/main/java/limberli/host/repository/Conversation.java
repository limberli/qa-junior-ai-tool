package limberli.host.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_text", columnDefinition = "TEXT", nullable = false)
    private String documentText;

    @Column(name = "tester_response", columnDefinition = "TEXT")
    private String testerResponse;

    @Column(name = "analyst_response", columnDefinition = "TEXT")
    private String analystResponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
