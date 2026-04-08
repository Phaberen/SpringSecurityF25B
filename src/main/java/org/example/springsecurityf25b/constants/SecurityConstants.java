package org.example.springsecurityf25b.constants;

public interface SecurityConstants {

    // secret and expiration are used at authentication stage.
    // header is used in authorization stage.

    // constants should be ALL_CAPS_WITH_UNDERSCORES to signal that they are constants and should not be modified
    // JWT_SECRET should hold complex combination of characters
    String JWT_SECRET = "98127871491208sfsdfa31209381209asdf381#209381203ff981#20398s#120398120398";

    // JWT_HEADER should be "Authorization" as it is
    // the standard header for passing JWT tokens in HTTP requests
    String JWT_HEADER = "Authorization";

    // Token expiration time
    long JWT_EXPIRATION = 1000 * 60 * 60; // 1 hour in milliseconds
}