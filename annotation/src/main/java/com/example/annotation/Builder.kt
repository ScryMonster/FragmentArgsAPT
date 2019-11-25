package com.example.annotation

import java.lang.annotation.RetentionPolicy


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Builder