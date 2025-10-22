package com.springbootproject.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {
    
    // JWT密钥 - 使用更强安全的固定密钥，长度满足HS512算法要求
    private final SecretKey jwtSecretKey = Keys.hmacShaKeyFor("ThisIsASuperSecretKeyForJWTAuthentication2024WithSufficientLength1234567890!".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    
    // JWT过期时间（毫秒），这里设置为2小时
    private int jwtExpirationMs = 7200000;

    // 从token中获取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 从token中获取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 从token中获取特定的声明
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 从token中获取所有声明
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw e;
        }
    }

    // 检查token是否已过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 生成token
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, username);
    }

    // 实际生成token的方法
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 从token中提取用户名
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 验证token是否有效（不需要username参数）
     */
    public Boolean validateToken(String token) {
        try {
            System.out.println("开始验证token: " + (token != null && token.length() > 0 ? token.substring(0, 20) + "..." : "null"));
            
            // 先检查token是否为空
            if (token == null || token.isEmpty()) {
                System.out.println("Token为空，验证失败");
                return false;
            }
            
            // 验证token签名和格式
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 检查token是否过期
            boolean isExpired = isTokenExpired(token);
            System.out.println("Token验证结果 - 过期: " + isExpired + ", 用户名: " + claims.getSubject());
            
            return !isExpired;
        } catch (SignatureException e) {
            System.out.println("Token签名验证失败: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Token格式错误: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("Token已过期: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("不支持的Token类型: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Token参数非法: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Token验证过程中发生未知错误: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 验证token是否有效（兼容旧方法）
     */
    public Boolean validateToken(String token, String username) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String tokenUsername = claims.getSubject();
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    // 获取token是否过期的错误类型
    public boolean isTokenExpiredError(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}