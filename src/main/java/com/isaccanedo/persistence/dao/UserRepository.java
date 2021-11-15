package com.isaccanedo.persistence.dao;

import com.isaccanedo.persistence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
