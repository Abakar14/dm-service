package com.bytmasoft;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtTokenUtil {


  private static final String secret = "8KvVu4Bn2KwBNX1MdnXY6VaRVLekVxurk3aaJxnzdCuZL6AHEL0EnnAOYLZtHWUr";
  private static final Long accessTokenExpiration = 1800000L;

  private static final String date_Format_custom = "dd.MM.yyyy";
  private static final String date_Format_db = "yyyy-MM-dd";


  public static String generateToken(String username, List<String> roles) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", roles);
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(SignatureAlgorithm.HS256, secret)
        .compact();
  }

  public static String generateToken() {
    List<String> roles = new ArrayList<>();
    roles.add("ROLE_ADMIN");
    roles.add("ROLE_TEACHER");
    return com.bytmasoft.dm.service.JwtTokenUtil.generateToken("abakar", roles);
  }

  public static Date getBirthDate(String dateInString) throws ParseException {

    SimpleDateFormat formatter = new SimpleDateFormat(date_Format_db);
    Date date = formatter.parse(dateInString);
    return date;
  }

  static String getBirthDateAsString(Date date) {
    return date != null ? new SimpleDateFormat(date_Format_custom).format(date) : null;
  }


}
