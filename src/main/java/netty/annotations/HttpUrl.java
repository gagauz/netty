package netty.annotations;

import netty.consts.HttpMethods;

public @interface HttpUrl {
    String value();

    HttpMethods[] methods() default {};

    String[] consume() default {};

    String produce() default "";
}
