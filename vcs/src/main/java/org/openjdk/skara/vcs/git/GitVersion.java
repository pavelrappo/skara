/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.skara.vcs.git;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

import org.openjdk.skara.process.Process;

public class GitVersion {

    private static final Pattern versionPattern = Pattern.compile(
            "git version (?<versionString>.*?(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<security>\\d+).*)");

    private final String versionString;
    private final int major;
    private final int minor;
    private final int security;

    private GitVersion(String versionString, int major, int minor, int security) {
        this.versionString = versionString;
        this.major = major;
        this.minor = minor;
        this.security = security;
    }

    /* exposed for testing */
    protected static GitVersion parse(String version) {
        var matcher = versionPattern.matcher(version);
        if (!matcher.find()) {
            return null;
        }

        return new GitVersion(
            matcher.group("versionString"),
            Integer.parseInt(matcher.group("major")),
            Integer.parseInt(matcher.group("minor")),
            Integer.parseInt(matcher.group("security"))
        );
    }

    public static GitVersion get() throws IOException {
        try (var e = Process.capture("git", "--version").execute()) {
            var r = e.await();
            var version = r.stdout().stream()
                    .map(GitVersion::parse)
                    .filter(Objects::nonNull)
                    .findFirst();
            if (r.status() != 0 || version.isEmpty()) {
                throw new IOException("Couldn't determine git version. [stdout] "
                        + r.stdout() + "\n[stderr] " + r.stderr());
            } else {
                return version.get();
            }
        }
    }


    public boolean isKnownSupported() {
        if (major < 2) {
            return false;
        }

        switch (minor) {
//            case 17:
//            case 19:
//                return security >= 4;
//
//            case 18:
//            case 20:
            case 22: // we require 2.22 since we use --combined-all-paths option of git log
            case 25:
                return security >= 3;

//            case 21:
            case 23:
            case 24:
                return security >= 2;

            default: {
                if (minor >= 26) {
                    return true;
                }
            }
        }

        return false;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int security() {
        return security;
    }

    @Override
    public String toString() {
        return versionString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitVersion that = (GitVersion) o;
        return Objects.equals(versionString, that.versionString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionString);
    }
}
