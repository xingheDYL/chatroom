package com.chat.service;

import com.chat.dto.*;
import com.chat.entity.User;
import com.chat.repository.UserRepository;
import com.chat.util.JwtUtil;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务类
 *
 * 处理用户注册、登录和邮箱验证
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int CODE_LENGTH = 6;

    /**
     * 用户名密码注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("offline")
                .build();

        user = userRepository.save(user);

        // 生成令牌
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 构建响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .user(buildUserDto(user))
                .build();
    }

    /**
     * 用户名密码登录
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 从仓库获取用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新最后登录时间和状态为在线
        user.setLastLoginAt(LocalDateTime.now());
        user.setStatus("online");
        userRepository.save(user);

        // 生成令牌
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 构建响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .user(buildUserDto(user))
                .build();
    }

    /**
     * 发送邮箱验证码
     */
    public void sendVerificationCode(SendCodeRequest request) {
        String email = request.getEmail();

        // 检查是否存在使用此邮箱的用户
        if (!userRepository.existsByEmail(email)) {
            // 为了安全，即使邮箱不存在也发送成功响应
            // 但可以选择不发送实际邮件
        }

        // 生成验证码
        String code = generateVerificationCode();

        // 在Redis中存储验证码并设置过期时间
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("1329749225@qq.com");  // 设置发件人地址，必须与授权账号一致
            message.setTo(email);
            message.setSubject("聊天室验证码");
            message.setText("您的验证码是: " + code + "\n\n" +
                    "此验证码将在 " + CODE_EXPIRATION_MINUTES + " 分钟后过期。");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("发送验证码邮件失败", e);
        }
    }

    /**
     * 邮箱验证码登录
     */
    @Transactional
    public AuthResponse loginWithCode(LoginWithCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        // 获取存储的验证码
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new RuntimeException("验证码已过期或未请求");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("验证码不正确");
        }

        // 删除已使用的验证码
        redisTemplate.delete(key);

        // 获取或创建用户
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 如果用户不存在则创建新用户
                    String username = "user_" + System.currentTimeMillis();
                    User newUser = User.builder()
                            .username(username)
                            .email(email)
                            .password(passwordEncoder.encode(generateRandomPassword()))
                            .status("offline")
                            .build();
                    return userRepository.save(newUser);
                });

        // 更新最后登录时间和状态为在线
        user.setLastLoginAt(LocalDateTime.now());
        user.setStatus("online");
        userRepository.save(user);

        // 生成令牌
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 构建响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .user(buildUserDto(user))
                .build();
    }

    /**
     * 用户登出
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("offline");
        userRepository.save(user);
    }

    /**
     * 修改用户密码
     */
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 生成随机验证码
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 生成随机密码（用于仅邮箱登录的用户）
     */
    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * 从实体构建用户DTO
     */
    private AuthResponse.UserDto buildUserDto(User user) {
        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .build();
    }
}
