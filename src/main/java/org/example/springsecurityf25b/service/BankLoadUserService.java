package org.example.springsecurityf25b.service;

import org.example.springsecurityf25b.model.Customer;
import org.example.springsecurityf25b.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BankLoadUserService implements UserDetailsService {

    @Autowired
    CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String userName, password = null;
        List<GrantedAuthority> authorities = null;
        Optional<Customer> customer = customerRepository.findByEmail(username);
        if (customer.isPresent()) {
            userName = customer.get().getEmail();
            password = customer.get().getPwd();
            authorities = new ArrayList<>();
            String[] roles = customer.get().getRole().split(",");
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(role.trim()));
            }
    } else {
            throw new UsernameNotFoundException("User details not found for the user:" + username);
        }
        return new User(username,password, authorities);
    }
}
