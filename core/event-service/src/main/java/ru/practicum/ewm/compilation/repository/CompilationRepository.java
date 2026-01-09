package ru.practicum.ewm.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.compilation.Compilation;


@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> getAllByPinned(boolean pinned, Pageable pageable);
}
