package com.knotnote.backend.repository;

import com.knotnote.backend.entity.SmartFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmartFolderRepository extends JpaRepository<SmartFolder, Long> {

    List<SmartFolder> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<SmartFolder> findByIdAndUserId(Long id, Long userId);
}
