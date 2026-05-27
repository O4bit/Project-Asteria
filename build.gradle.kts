// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}


buildscript {
    dependencies {
        constraints {
            // Netty — force every published module to the patched line.
            listOf(
                "netty-all",
                "netty-buffer",
                "netty-codec",
                "netty-codec-dns",
                "netty-codec-http",
                "netty-codec-http2",
                "netty-codec-socks",
                "netty-common",
                "netty-handler",
                "netty-handler-proxy",
                "netty-resolver",
                "netty-resolver-dns",
                "netty-transport",
                "netty-transport-classes-epoll",
                "netty-transport-native-epoll",
                "netty-transport-native-unix-common",
            ).forEach { artifact ->
                classpath("io.netty:$artifact") {
                    version { require("4.1.132.Final") }
                    because("Pin to patched Netty (MadeYouReset CVE-2025-55163, CONTINUATION flood, decompression bombs, Lz4 resource exhaustion, request smuggling, SslHandler native crash, Rapid Reset, HttpClientCodec desync)")
                }
            }

            // Bouncy Castle — covert timing channel + LDAP injection + broken algo.
            listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on", "bcjmail-jdk18on", "bctls-jdk18on", "bcmail-jdk18on").forEach { artifact ->
                classpath("org.bouncycastle:$artifact") {
                    version { require("1.84") }
                    because("Pin Bouncy Castle to 1.84 to clear High-severity covert timing channel (CVE-2026-5598) and related advisories")
                }
            }

            // JDOM — XXE in SAXBuilder.
            classpath("org.jdom:jdom2") {
                version { require("2.0.6.1") }
                because("Pin JDOM2 to 2.0.6.1 to fix XXE Injection (CVE-2021-33813)")
            }

            // jose4j — DoS via compressed JWE.
            classpath("org.bitbucket.b_c:jose4j") {
                version { require("0.9.6") }
                because("Pin jose4j to 0.9.6 to fix DoS via compressed JWE content (CVE-2024-29371)")
            }
        }
    }
}

// The above pins the *root* buildscript classpath. Several vulnerable jars
// (netty 4.1.93/4.1.110 via gRPC, bouncycastle 1.79 via build-tools, etc.)
// are pulled into the *app module's* lint / aapt2 / analyzer configurations
// instead. Apply the same constraints there via a resolutionStrategy on all
// configurations of every subproject.
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "io.netty" -> {
                    if (requested.name.startsWith("netty")) {
                        useVersion("4.1.132.Final")
                        because("Force-upgrade Netty to clear High-severity CVEs (MadeYouReset, CONTINUATION flood, decompression bombs, Lz4, request smuggling, SslHandler native crash, Rapid Reset, HttpClientCodec desync)")
                    }
                }
                "org.bouncycastle" -> {
                    if (requested.name.endsWith("-jdk18on")) {
                        useVersion("1.84")
                        because("Force-upgrade Bouncy Castle to 1.84 (covert timing channel CVE-2026-5598 and related)")
                    }
                }
                "org.jdom" -> {
                    if (requested.name == "jdom2") {
                        useVersion("2.0.6.1")
                        because("Force-upgrade JDOM2 to 2.0.6.1 (XXE CVE-2021-33813)")
                    }
                }
                "org.bitbucket.b_c" -> {
                    if (requested.name == "jose4j") {
                        useVersion("0.9.6")
                        because("Force-upgrade jose4j to 0.9.6 (DoS via compressed JWE CVE-2024-29371)")
                    }
                }
            }
        }
    }
}

