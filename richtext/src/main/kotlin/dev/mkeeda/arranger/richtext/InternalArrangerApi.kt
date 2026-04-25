package dev.mkeeda.arranger.richtext

@RequiresOptIn(
    message = "This API is internal to Arranger and should not be used outside of it.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalArrangerApi
