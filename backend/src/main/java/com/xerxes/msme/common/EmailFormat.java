package com.xerxes.msme.common;

/**
 * The email format a new account must have: a domain with a dot and a letters-only ending ("name@example.com"). Stricter
 * than Jakarta's {@code @Email}, which also accepts "a@b" and "user@localhost". Only for registering a new account -
 * login must stay lenient so an existing account with an unusual address can still sign in.
 */
public final class EmailFormat {

    public static final String REGEX = "^[^\\s@]+@[^\\s@]+\\.[A-Za-z]{2,}$";
    public static final String MESSAGE = "email must be a valid address like name@example.com";

    private EmailFormat() {}
}
