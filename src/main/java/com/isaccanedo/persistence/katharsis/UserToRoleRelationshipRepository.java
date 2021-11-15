package com.isaccanedo.persistence.katharsis;

import com.isaccanedo.persistence.dao.RoleRepository;
import com.isaccanedo.persistence.dao.UserRepository;
import com.isaccanedo.persistence.model.User;
import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.RelationshipRepositoryV2;
import io.katharsis.resource.list.ResourceList;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.isaccanedo.persistence.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserToRoleRelationshipRepository implements RelationshipRepositoryV2<User, Long, Role, Long> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void setRelation(User User, Long roleId, String fieldName) {
        // not for many-to-many
    }

    @Override
    public void setRelations(User user, Iterable<Long> roleIds, String fieldName) {
        final Set<Role> roles = new HashSet<Role>();
        roles.addAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public void addRelations(User user, Iterable<Long> roleIds, String fieldName) {
        final Set<Role> roles = user.getRoles();
        roles.addAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public void removeRelations(User user, Iterable<Long> roleIds, String fieldName) {
        final Set<Role> roles = user.getRoles();
        roles.removeAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public Role findOneTarget(Long sourceId, String fieldName, QuerySpec querySpec) {
        // not for many-to-many
        return null;
    }

    @Override
    public ResourceList<Role> findManyTargets(Long sourceId, String fieldName, QuerySpec querySpec) {
        final Optional<User> userOptional = userRepository.findById(sourceId);
        User user = userOptional.isPresent() ? userOptional.get() : new User();
        return  querySpec.apply(user.getRoles());
    }

    @Override
    public Class<User> getSourceResourceClass() {
        return User.class;
    }

    @Override
    public Class<Role> getTargetResourceClass() {
        return Role.class;
    }

}
