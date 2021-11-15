package com.isaccanedo.persistence.dao;

import com.isaccanedo.persistence.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

}
