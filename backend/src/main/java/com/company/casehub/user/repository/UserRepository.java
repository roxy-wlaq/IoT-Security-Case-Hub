package com.company.casehub.user.repository;

import com.company.casehub.user.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    /**
     * Exact-case lookup. Used by the login path so that authentication is
     * case-sensitive: only the stored canonical casing of a username
     * authenticates (e.g. {@code admin} succeeds, {@code Admin}/{@code ADMIN}
     * fail).
     */
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
