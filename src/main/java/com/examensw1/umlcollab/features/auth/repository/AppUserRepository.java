package com.examensw1.umlcollab.features.auth.repository;
import com.examensw1.umlcollab.features.auth.model.AppUser; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser,UUID>{ Optional<AppUser> findByEmail(String email); boolean existsByEmail(String email); }
