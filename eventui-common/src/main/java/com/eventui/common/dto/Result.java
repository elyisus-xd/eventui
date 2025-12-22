package com.eventui.common.dto;

/**
 * Representa el resultado de una operación que puede fallar.
 * Pattern Result para manejo de errores sin excepciones.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {
        /**
         * @return true siempre (para checks)
         */
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure<T>(String reason) implements Result<T> {
        /**
         * @return false siempre (para checks)
         */
        public boolean isSuccess() {
            return false;
        }
    }

    /**
     * @return true si el resultado es Success
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * @return true si el resultado es Failure
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Extrae el valor si es Success, lanza excepción si es Failure.
     */
    @SuppressWarnings("unchecked")
    default T unwrap() {
        if (this instanceof Success<T> success) {
            return success.value();
        }
        throw new IllegalStateException("Attempted to unwrap a Failure result: " +
                ((Failure<T>) this).reason());
    }

    /**
     * Extrae el mensaje de error si es Failure.
     */
    @SuppressWarnings("unchecked")
    default String getError() {
        if (this instanceof Failure<T> failure) {
            return failure.reason();
        }
        return "";
    }
}
