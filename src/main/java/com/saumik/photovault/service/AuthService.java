package com.saumik.photovault.service;


import com.saumik.photovault.dto.AuthResponse;
import com.saumik.photovault.dto.LoginRequest;
import com.saumik.photovault.dto.RegisterRequest;
import com.saumik.photovault.dto.UserResponse;
import com.saumik.photovault.entity.User;
import com.saumik.photovault.event.DomainEventPublisher;
import com.saumik.photovault.event.PasswordResetRequestedEvent;
import com.saumik.photovault.event.UserRegisteredEvent;
import com.saumik.photovault.exception.EmailAlreadyExistsException;
import com.saumik.photovault.exception.UserNotFoundException;
import com.saumik.photovault.repository.UserRepository;
import com.saumik.photovault.security.JwtService;
import com.saumik.photovault.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final PasswordResetService passwordResetService;

    private final EmailVerificationService emailVerificationService;

    private final DomainEventPublisher eventPublisher;


    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiry;


    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exist!");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();

        userRepository.save(user);

        eventPublisher.publish(
                new UserRegisteredEvent(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail()
                )
        );

    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        // automatic handling in AppUserDetails
//        if (!user.isEmailVerified()) {
//            throw new EmailNotVerifiedException(
//                    "Please verify your email before logging in."
//            );
//        }

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                refreshTokenService.createRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName()
                )
        );
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {

        UUID userId =
                refreshTokenService.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        refreshTokenService.revokeRefreshToken(userId,refreshToken);

        String newRefresh =
                refreshTokenService.createRefreshToken(userId);

        String access =
                jwtService.generateAccessToken(user);

        return new AuthResponse(
                access,
                newRefresh,
                new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName()
                )
        );
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null) {
            return; // silent
        }
        eventPublisher.publish(
                new PasswordResetRequestedEvent(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail()
                )
        );
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {

        UUID userId = passwordResetService.validateResetToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        refreshTokenService.revokeAllRefreshTokens(userId);

        passwordResetService.deleteResetToken(token);
    }

    @Transactional
    public void verifyEmail(String token) {
        UUID userId = emailVerificationService.getId(token);
        if(userId == null){
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if(user == null){
            return; // silent
        }
        user.verifyEmail();
        emailVerificationService.deleteToken(token);
    }

    public void resendVerificationEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if(user == null || user.isEmailVerified()) {
            return; // silent
        }

        eventPublisher.publish(
                new UserRegisteredEvent(
                        user.getId(),
                        user.getDisplayName(),
                        user.getEmail()
                )
        );
    }


}