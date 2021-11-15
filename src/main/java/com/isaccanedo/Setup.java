package com.isaccanedo;

import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import com.isaccanedo.persistence.dao.RoleRepository;
import com.isaccanedo.persistence.dao.UserRepository;
import com.isaccanedo.persistence.model.Role;
import com.isaccanedo.persistence.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Setup {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    private void setupData() {
        Role roleUser = new Role("ROLE_USER");
        roleUser = roleRepository.save(roleUser);
        Role roleAdmin = new Role("ROLE_ADMIN");
        roleAdmin = roleRepository.save(roleAdmin);

        final User userJohn = new User("john", "john@test.com");
        userJohn.setRoles(new HashSet<Role>(Arrays.asList(roleUser, roleAdmin)));
        userRepository.save(userJohn);

        final User userTom = new User("tom", "tom@test.com");
        userTom.setRoles(new HashSet<Role>(Arrays.asList(roleUser)));
        userRepository.save(userTom);
    }

}