package handlers.annotations;

public @interface Result {
    boolean id() default false;
    String property();
    String column();
}
