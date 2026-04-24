package com.skinsshowcase.auth.repository;

import com.skinsshowcase.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByDisplayName(String displayName);
}
