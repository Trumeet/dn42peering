/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * https://raw.githubusercontent.com/WireGuard/wireguard-android/5b5ba88a97b5310c532b42b526f78d1c747965f8/tunnel/src/main/java/com/wireguard/util/NonNullForAll.java
 */

package com.wireguard.util;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be applied to a package, class or method to indicate that all
 * class fields and method parameters and return values in that element are nonnull
 * by default unless overridden.
 */
@Nonnull
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)

public @interface NonNullForAll {
}