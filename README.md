### Este módulo contém artigos sobre Spring com Katharsis

# 1. Visão Geral
Neste artigo, começaremos a explorar a especificação JSON-API e como ela pode ser integrada a uma API REST apoiada por Spring.

Usaremos a implementação Katharsis de JSON-API em Java - e configuraremos um aplicativo Spring baseado em Katharsis - portanto, tudo o que precisamos é um aplicativo Spring.

# 2. Maven
Primeiro, vamos dar uma olhada em nossa configuração de maven - precisamos adicionar a seguinte dependência em nosso pom.xml:

```
<dependency>
    <groupId>io.katharsis</groupId>
    <artifactId>katharsis-spring</artifactId>
    <version>3.0.2</version>
</dependency>
```

# 3. Um recurso de usuário
A seguir, vamos dar uma olhada em nosso recurso de usuário:

```
@JsonApiResource(type = "users")
public class User {

    @JsonApiId
    private Long id;

    private String name;

    private String email;
}
```

Observe que:

- A anotação @JsonApiResource é usada para definir nosso usuário de recurso;
- A anotação @JsonApiId é usada para definir o identificador de recurso.

E muito brevemente - a persistência para este exemplo será um repositório Spring Data aqui:

```
public interface UserRepository extends JpaRepository<User, Long> {}
```

# 4. Um Repositório de Recursos
A seguir, vamos discutir nosso repositório de recursos - cada recurso deve ter um ResourceRepositoryV2 para publicar as operações de API disponíveis nele:

```
@Component
public class UserResourceRepository implements ResourceRepositoryV2<User, Long> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findOne(Long id, QuerySpec querySpec) {
        Optional<User> user = userRepository.findById(id); 
        return user.isPresent()? user.get() : null;
    }

    @Override
    public ResourceList<User> findAll(QuerySpec querySpec) {
        return querySpec.apply(userRepository.findAll());
    }

    @Override
    public ResourceList<User> findAll(Iterable<Long> ids, QuerySpec querySpec) {
        return querySpec.apply(userRepository.findAllById(ids));
    }

    @Override
    public <S extends User> S save(S entity) {
        return userRepository.save(entity);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Class<User> getResourceClass() {
        return User.class;
    }

    @Override
    public <S extends User> S create(S entity) {
        return save(entity);
    }
}
```

Uma nota rápida aqui - é claro que é muito semelhante a um controlador Spring.

# 5. Configuração Katharsis
Como estamos usando katharsis-spring, tudo o que precisamos fazer é importar KatharsisConfigV3 em nosso aplicativo Spring Boot:

```
@Import(KatharsisConfigV3.class)
```

E configure os parâmetros do Katharsis em nosso application.properties:

```
katharsis.domainName=http://localhost:8080
katharsis.pathPrefix=/
```

With that – we can now start consuming the API; for example:

GET “http://localhost:8080/users“: to get all users.
POST “http://localhost:8080/users“: to add new user, and more.

# 6. Relationships
Next, let's discuss how to handle entities relationships in our JSON API.

### 6.1. Role Resource
First, let's introduce a new resource – Role:

```
@JsonApiResource(type = "roles")
public class Role {

    @JsonApiId
    private Long id;

    private String name;

    @JsonApiRelation
    private Set<User> users;
}
```

E, em seguida, configure uma relação muitos para muitos entre o usuário e a função:

```
@JsonApiRelation(serialize=SerializeType.EAGER)
private Set<Role> roles;
```

### 6.2 Repositório de recursos de funções
Muito rapidamente - aqui está nosso repositório de recursos de função:

```
@Component
public class RoleResourceRepository implements ResourceRepositoryV2<Role, Long> {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Role findOne(Long id, QuerySpec querySpec) {
        Optional<Role> role = roleRepository.findById(id); 
        return role.isPresent()? role.get() : null;
    }

    @Override
    public ResourceList<Role> findAll(QuerySpec querySpec) {
        return querySpec.apply(roleRepository.findAll());
    }

    @Override
    public ResourceList<Role> findAll(Iterable<Long> ids, QuerySpec querySpec) {
        return querySpec.apply(roleRepository.findAllById(ids));
    }

    @Override
    public <S extends Role> S save(S entity) {
        return roleRepository.save(entity);
    }

    @Override
    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    public Class<Role> getResourceClass() {
        return Role.class;
    }

    @Override
    public <S extends Role> S create(S entity) {
        return save(entity);
    }
}
```

É importante entender aqui que esse repo de recurso único não lida com o aspecto do relacionamento - isso leva um repositório separado.

### 6.3. Repositório de Relacionamento
Para lidar com a relação muitos para muitos entre o usuário e a função, precisamos criar um novo estilo de repositório:

```
@Component
public class UserToRoleRelationshipRepository implements RelationshipRepositoryV2<User, Long, Role, Long> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void setRelation(User User, Long roleId, String fieldName) {}

    @Override
    public void setRelations(User user, Iterable<Long> roleIds, String fieldName) {
        Set<Role> roles = new HashSet<Role>();
        roles.addAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public void addRelations(User user, Iterable<Long> roleIds, String fieldName) {
        Set<Role> roles = user.getRoles();
        roles.addAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public void removeRelations(User user, Iterable<Long> roleIds, String fieldName) {
        Set<Role> roles = user.getRoles();
        roles.removeAll(roleRepository.findAllById(roleIds));
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public Role findOneTarget(Long sourceId, String fieldName, QuerySpec querySpec) {
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
```

Estamos ignorando os métodos singulares aqui, no repositório de relacionamento.

# 7. Teste
Por fim, vamos analisar algumas solicitações e realmente entender a aparência da saída JSON-API.

Vamos começar a recuperar um único recurso de usuário (com id = 2):

```
GET http://localhost:8080/users/2
```

```
{
    "data":{
        "type":"users",
        "id":"2",
        "attributes":{
            "email":"tom@test.com",
            "username":"tom"
        },
        "relationships":{
            "roles":{
                "links":{
                    "self":"http://localhost:8080/users/2/relationships/roles",
                    "related":"http://localhost:8080/users/2/roles"
                }
            }
        },
        "links":{
            "self":"http://localhost:8080/users/2"
        }
    },
    "included":[
        {
            "type":"roles",
            "id":"1",
            "attributes":{
                "name":"ROLE_USER"
            },
            "relationships":{
                "users":{
                    "links":{
                        "self":"http://localhost:8080/roles/1/relationships/users",
                        "related":"http://localhost:8080/roles/1/users"
                    }
                }
            },
            "links":{
                "self":"http://localhost:8080/roles/1"
            }
        }
    ]
}
```

Aprendizado:

- Os principais atributos do Recurso encontram-se em data.attributes;
- As principais relações do Recurso encontram-se em data.relationships;
- Como usamos @JsonApiRelation (serialize = SerializeType.EAGER) para o relacionamento de funções, ele é incluído no JSON e encontrado no nó incluído.

Em seguida, vamos obter o recurso de coleta que contém as funções:

```
GET http://localhost:8080/roles
```

```
{
    "data":[
        {
            "type":"roles",
            "id":"1",
            "attributes":{
                "name":"ROLE_USER"
            },
            "relationships":{
                "users":{
                    "links":{
                        "self":"http://localhost:8080/roles/1/relationships/users",
                        "related":"http://localhost:8080/roles/1/users"
                    }
                }
            },
            "links":{
                "self":"http://localhost:8080/roles/1"
            }
        },
        {
            "type":"roles",
            "id":"2",
            "attributes":{
                "name":"ROLE_ADMIN"
            },
            "relationships":{
                "users":{
                    "links":{
                        "self":"http://localhost:8080/roles/2/relationships/users",
                        "related":"http://localhost:8080/roles/2/users"
                    }
                }
            },
            "links":{
                "self":"http://localhost:8080/roles/2"
            }
        }
    ],
    "included":[

    ]
}
```

A conclusão rápida aqui é que obtemos todas as funções no sistema - como uma matriz no nó de dados

# 8. Conclusão
JSON-API é uma especificação fantástica - finalmente adicionando alguma estrutura na maneira como usamos JSON em nossas APIs e realmente potencializando uma verdadeira API de hipermídia.

Este artigo explorou uma maneira de configurá-lo em um aplicativo Spring. Mas, independentemente dessa implementação, a especificação em si é - na minha opinião - um trabalho muito promissor.