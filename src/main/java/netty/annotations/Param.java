package netty.annotations;

public @interface Param {
    String name();

    boolean raw() default false;
}
