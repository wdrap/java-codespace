package dev.wdrap.monands;

import java.net.URL;
import java.util.function.Function;

class User {
    private final String id;
    private final String name;

    User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

class Authenticator {
    // https://www.youtube.com/watch?v=_ykDhFYRaQ8&t=305s
    public User login(String id, String pwd) throws Exception {
        System.out.println("Inside system login");
        throw new Exception("password mismatch");
        // return new User(id, "dwrap");
    }

    public User gmailLogin(String id, String pwd) throws Exception {
        System.out.println("Inside gmail login");
        // throw new IIOException("some problem");
        return new User(id, "wdrap");
    }

    public void twoFactor(User user, long pwd) {
        System.out.println("Inside twoFactor: " + user.getId());
        // throw new RuntimeException("twoFactor Incorrect key");
    }
}

class Dispatcher {
    static void redirect(URL target) {
        System.out.println("Going to => " + target);
    }
}

interface SupplierThrowsException<T, E extends Exception> {
    T get() throws E;
}

abstract class Try<T> {
    abstract T get();

    static <T, E extends Exception> Try<T> with(SupplierThrowsException<T, E> codeBlock) {
        try {
            return new Success<>(codeBlock.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    abstract <U> Try<U> chain(Function<T, Try<U>> f);

    abstract <U> Try<U> recoverWith(Function<Exception, Try<U>> f);

    abstract <U> Try<U> orElse(Try<U> other);
}

class Success<T> extends Try<T> {
    private final T value;

    Success(T value) {
        this.value = value;
    }

    @Override
    T get() {
        return value;
    }

    @Override
    <U> Try<U> chain(Function<T, Try<U>> f) {
        try {
            return f.apply(value);
        } catch (Exception e) {
            return new Failure<U>(e);
        }
    }

    @Override
    <U> Try<U> recoverWith(Function<Exception, Try<U>> f) {
        return (Try<U>) this;
    }

    @Override
    <U> Try<U> orElse(Try<U> other) {
        return (Try<U>) this;
    }
}

class Failure<T> extends Try<T> {
    private final Exception e;

    Failure(Exception e) {
        this.e = e;
    }

    T get() {
        throw new RuntimeException(e);
    }

    @Override
    <U> Try<U> chain(Function<T, Try<U>> f) {
        return (Try<U>) this;
    }

    @Override
    <U> Try<U> recoverWith(Function<Exception, Try<U>> f) {
        try {
            return f.apply(e);
        } catch (Exception e) {
            return new Failure<U>(e);
        }
    }

    @Override
    <U> Try<U> orElse(Try<U> other) {
        return other;
    }
}

public class SuccessFailurePipeline {
    public static void main(String[] args) {
        final String userid = "userid";
        final String pwd = "pwd";

        Authenticator authenticator = new Authenticator();

        Try<URL> target = Try.with(() -> authenticator.login(userid, pwd))
                .recoverWith(e -> Try.with(() -> authenticator.gmailLogin(userid, pwd))
                .chain(user -> Try.with(() -> {
                    authenticator.twoFactor(user, 1234);
                    return new URL("http://dashboard");
                }))
                .orElse(Try.with(() -> new URL("http://login"))));

        Dispatcher.redirect(target.get());
        System.out.println("DONE");
    }

    // public static void main(String[] args) throws Exception {
    //     final URL dashboard = new URL("http://dashboard");
    //     final URL loginPage = new URL("http://login");
 
    //     final String userid = "userid";
    //     final String pwd = "pwd";
    //     User user;

    //     Authenticator authenticator = new Authenticator();

    //     try {
    //         user = authenticator.login(userid, pwd);
    //     } catch (Exception e) {
    //         System.out.println("system login failed: " + e.getMessage());
    //         try {
    //             user = authenticator.gmailLogin(userid, pwd);
    //         } catch (Exception eg) {
    //             System.out.println("gmail login failed: " + eg.getMessage());
    //             Dispatcher.redirect(loginPage);
    //             return;
    //         }
    //     }
    //     URL target;
    //     try {
    //         long twoFactorPwd = 1234;
    //         authenticator.twoFactor(user, twoFactorPwd);
    //         target = dashboard;
    //     } catch (Exception e) {
    //         target = loginPage;
    //     }
    //     Dispatcher.redirect(target);
    //     System.out.println("DONE");
    // }

    // links https://github.com/DhavalDalal/Java8-Try and http://dhavaldalal.github.io/Java8-Try/
}
