/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hanielfialho.api.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.source.PermissionHolder;
import org.junit.jupiter.api.Test;

final class CommandRequirementTest {

    @Test
    void requirementAlwaysTrue() {
        PermissionHolder holder = perm -> true;
        assertThat(holder.hasPermission("")).isTrue();
        assertThat(holder.hasPermission("any.permission")).isTrue();
    }

    @Test
    void requirementAlwaysFalse() {
        PermissionHolder holder = perm -> false;
        assertThat(holder.hasPermission("unknown.command")).isFalse();
    }

    @Test
    void multipleRequirements() {
        PermissionHolder holder = perm -> perm.equals("admin") || perm.equals("moderator");
        assertThat(holder.hasPermission("admin")).isTrue();
        assertThat(holder.hasPermission("moderator")).isTrue();
        assertThat(holder.hasPermission("user")).isFalse();
    }

    @Test
    void errorInRequirement() {
        PermissionHolder holder = perm -> {
            throw new RuntimeException("permission check failed");
        };
        assertThatThrownBy(() -> holder.hasPermission("anything"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("permission check failed");
    }
}
