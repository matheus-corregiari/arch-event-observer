package br.com.arch.toolkit.compose

/**
 * Marks the [DataResultContentScope] DSL and its observer registration functions.
 *
 * Nested content scopes resolve implicit calls against the nearest scope.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class DataResultContentDsl
