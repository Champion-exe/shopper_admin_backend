package com.shopper.repository;

import com.shopper.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);
}
