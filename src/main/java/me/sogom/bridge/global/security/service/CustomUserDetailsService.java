package me.sogom.bridge.global.security.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return parentRepository.findByEmail(email)
                .<UserDetails>map(p -> new AuthMember(p, MemberRole.PARENT))
                .or(() -> childrenRepository.findByEmail(email)
                        .map(c -> new AuthMember(c, MemberRole.CHILDREN)))
                .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + email));
    }
}
