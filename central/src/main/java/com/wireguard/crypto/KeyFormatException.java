/*
 * Copyright © 2018-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * https://raw.githubusercontent.com/WireGuard/wireguard-android/5b5ba88a97b5310c532b42b526f78d1c747965f8/tunnel/src/main/java/com/wireguard/crypto/KeyFormatException.java
 */

package com.wireguard.crypto;

import com.wireguard.util.NonNullForAll;

/**
 * An exception thrown when attempting to parse an invalid key (too short, too long, or byte
 * data inappropriate for the format). The format being parsed can be accessed with the
 * {@link #getFormat} method.
 */
@NonNullForAll
public final class KeyFormatException extends Exception {
    private final Key.Format format;
    private final Type type;

    KeyFormatException(final Key.Format format, final Type type) {
        this.format = format;
        this.type = type;
    }

    public Key.Format getFormat() {
        return format;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        CONTENTS,
        LENGTH
    }
}