package core.ms.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import core.ms.security.domain.MSUser;
import core.ms.security.domain.MSUserRole;
import core.ms.security.exception.UsernameAlreadyExistException;
import core.ms.security.DAO.MSUserDAO;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Optional;


@Service
public class MSUserService implements UserDetailsService {
    @Autowired
    MSUserDAO repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<MSUser> optMSUser = repository.findById(username);

        if(optMSUser.isPresent()){
            MSUser msUser = optMSUser.get();
            UserBuilder builder =
                        User.builder()
                            .username(msUser.getUsername())
                            .password(msUser.getUserPassword());
            HashSet<String> roles = new HashSet<>();
            switch (msUser.getRole()){
                case GUEST:
                    roles.add("GUEST");
                    break;
                case ADMIN:
                    roles.add("ADMIN");
                    break;
                case CUSTOMER:
                    roles.add("CUSTOMER");
                    break;
            }
            builder.roles(roles.toArray(new String[roles.size()]));
            UserDetails details = builder.build();
            return details;
        }else {
            throw new UsernameNotFoundException(username);
        }

    }
    public MSUser createUser(String username, String password, MSUserRole userRole) throws UsernameAlreadyExistException {
        if(repository.existsById(username)){
            throw new UsernameAlreadyExistException();
        }
        String encodedPassword = passwordEncoder.encode(password);
        MSUser newUser = new MSUser(username, MSUserRole.CUSTOMER, encodedPassword);
        return repository.save(newUser);
    }

}
