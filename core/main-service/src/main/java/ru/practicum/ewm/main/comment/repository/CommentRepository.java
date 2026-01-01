package ru.practicum.ewm.main.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.main.comment.model.Comment;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    long deleteCommentByIdAndAuthorId(Long id, Long authorId);

    Page<Comment> findAllByAuthorIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Comment> findAllByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);

    Page<Comment> findAllByAuthorIdAndEventIdOrderByCreatedAtDesc(Long userId, Long eventId, Pageable pageable);

}
