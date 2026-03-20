package sqz.checklist.common

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlRegexHelperUnitTest {

    @Test
    fun test_ipv4Regex() {
        val validAddresses = listOf(
            "0.0.0.0",
            "127.0.0.1",
            "192.168.1.1",
            "255.255.255.255",
            "1.2.3.4",
            "123.123.123.123",
            "10.0.0.254"
        )
        val invalidAddresses = listOf(
            "256.256.256.256",       // Out of range (> 255)
            "192.168.1",             // Too few octets
            "192.168.1.1.1",         // Too many octets
            "a.b.c.d",               // Non-numeric
            "192.168.01.1",          // Leading zeros (optional check, usually invalid)
            "192.168.1.256",         // Last octet out of range
            "",                      // Empty string
            "192.168.1.",            // Trailing dot
            ".192.168.1.1",          // Leading dot
            "192.168.1. 1",          // Contains space
            "300.0.0.0",             // First octet out of range
            "https://google.com/",   // It's a URL
            "google.com",            // It's a URL
            "http://192.168.0.1",    // It's a URL
            "https://[2001:db8::1]", // IPv6 address
        )
        validAddresses.forEach { address ->
            assertTrue(
                UrlRegexHelper.ipv4Regex.matches(address),
                "Should match valid IPv4: $address"
            )
        }
        invalidAddresses.forEach { address ->
            assertFalse(
                UrlRegexHelper.ipv4Regex.matches(address),
                "Should not match invalid IPv4: $address"
            )
        }
    }

    @Test
    fun test_ipv4UrlRegex() {
        val validAddresses = listOf(
            "http://0.0.0.0/",
            "https://127.0.0.1",
            "https://192.168.1.1/",
            "http://255.255.255.255",
            "http://1.2.3.4/test/",
            "http://123.123.123.123",
            "http://10.0.0.254"
        )
        val invalidAddresses = listOf(
            "http://256.256.256.256",     // Out of range (> 255)
            "http://192.168.1/",          // Too few octets
            "http://192.168.1.1.1",       // Too many octets
            "http://a.b.c.d",             // Non-numeric
            "http://192.168.01.1",        // Leading zeros (optional check, usually invalid)
            "http://192.168.1.256",       // Last octet out of range
            "",                           // Empty string
            "http://192.168.1.",          // Trailing dot
            "http://.192.168.1.1",        // Leading dot
            "http://192.168.1. 1",        // Contains space
            "http://300.0.0.0",           // First octet out of range
            "https://google.com/",        // It's a URL
            "google.com",                 // It's a URL
            "192.168.1.1",                // No http or https
            "https://[2001:db8::1]",      // IPv6 address
        )
        validAddresses.forEach { address ->
            assertTrue(
                UrlRegexHelper.ipv4UrlRegex.matches(address),
                "Should match valid IPv4: $address"
            )
        }
        invalidAddresses.forEach { address ->
            assertFalse(
                UrlRegexHelper.ipv4UrlRegex.matches(address),
                "Should not match invalid IPv4: $address"
            )
        }
    }

    @Test
    fun test_ipv6UrlRegex() {
        val validIpv6Urls = listOf(
            "http://[2001:db8:85a3:8d3:1319:8a2e:370:7348]/",       // Standard full address
            "https://[2001:db8::1]",                                // Compressed zeros
            "http://[::1]",                                         // Loopback address
            "http://[::1]:8080/",                                   // With port and path
            "https://user:password@[2001:db8::1]",                  // With user info
            "https://[2001:db8::1]/path?query=val#fragment",        // Path, query, and fragment
            "http://[::ffff:192.168.1.1]",                          // IPv4-mapped IPv6
            "http://[fe80::1%25eth0]",                              // Link-local with Zone ID (%25 is %)
            "HTTPS://[2001:DB8::A1B2]",                             // Uppercase (Case Insensitive test)
            "ftp://[2001:db8:0:0:0:0:0:1]:21",                      // FTP scheme and full octets
        )
        val invalidIpv6Urls = listOf(
            "http://2001:db8::1",                       // Invalid: IPv6 in URL must have brackets []
            "2001:db8::1",                              // Invalid: Missing scheme and brackets
            "http://[2001:db8::g123]",                  // Invalid: Non-hex character 'g'
            "http://[2001:db8:::1]",                    // Invalid: Triple colon
            "http://[1:2:3:4:5:6:7:8:9]",               // Invalid: Too many octets
            "http://[::ffff:300.1.1.1]",                // Invalid: IPv4 part out of range
            "https://google.com",                       // Invalid: This regex is specific to IPv6 hosts
            "http://[2001:db8:: 1]",                    // Invalid: Contains a space
            "http://[[2001:db8::1]]",                   // Invalid: Double brackets
            "http://[2001:db8::1]:655360",              // Invalid: Port number out of range (too long)
            "[2001:db8:85a3:8d3:1319:8a2e:370:7348]/",  // Invalid: Missing http
        )
        validIpv6Urls.forEach { url ->
            assertTrue(
                UrlRegexHelper.ipv6UrlRegex.matches(url),
                "Should match valid IPv6 URL: $url"
            )
        }

        invalidIpv6Urls.forEach { url ->
            assertFalse(
                UrlRegexHelper.ipv6UrlRegex.matches(url),
                "Should NOT match invalid IPv6 URL: $url"
            )
        }
    }

    @Test
    fun test_urlRegex() {
        val matches = listOf(
            "https://www.google.com",
            "http://example.com/api/v1/users",
            "kotlinlang.org",
            "ftp://files.download.net:21",
            "dev.staging.internal.myapp.io",
            "https://bücher.de",
            "https://search.com?q=android+studio&hl=en&gl=us",
            "https://developer.android.com/guide/index.html#top",
            "my-awesome-app.cloud-services.net",
            "s3-region-1.amazonaws.com"
        )
        val fails = listOf(
            "192.168.1.1",
            "http://1.1.1.1",
            "example.c",
            "http://localhost",
            "https://google..com",
            "-example.com",
            "example-.com",
            "https://google.",
            "ssh://myserver.com",
            "https://my site.com",
            "https://[2001:db8::1]",
        )
        matches.forEach {
            assertTrue(UrlRegexHelper.urlRegex.matches(it), "Should have matched: $it")
        }
        fails.forEach {
            assertFalse(UrlRegexHelper.urlRegex.matches(it), "Should NOT have matched: $it")
        }
    }
}
