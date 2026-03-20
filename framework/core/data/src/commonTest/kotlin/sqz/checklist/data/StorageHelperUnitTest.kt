package sqz.checklist.data

import sqz.checklist.data.storage.StorageHelper.illegalFileNameCharsRegex
import sqz.checklist.data.storage.StorageHelper.isDataPath
import sqz.checklist.data.storage.StorageHelper.isMediaPath
import sqz.checklist.data.storage.StorageHelper.isTempPath
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageHelperUnitTest {

    @Test
    fun test_functions_can_handle_test_environment() {
        "TEST".isTempPath()
        "TEST".isDataPath()
        "TEST".isMediaPath()
    }

    @Test
    fun test_isMediaPath_regex() {
        val testTurePathStrCases = listOf(
            "/media/type/file",
            "/a/b/media/type/file",
            "/media/abc/xyz.png",
            "/a/media/type/file/",
            "data/user/0/com.sqz.checklist/files/media/picture/IMG_8964.jpg",
            "/media/picture/IMG_taiwanIsNation.jpg",
            "/a/media/type/media_file/",
        )
        val testFalsePathStrCases = listOf(
            "/MEDIA/type/file",
            "/media/type/other/file",
            "",
            "/a/media/type/file/media/x/y",
            "/media/type",
            "/media/type/file/other",
            "114514",
            "/do/you/like/e2/or/t",
        )
        testTurePathStrCases.forEach {
            assertTrue { it.isMediaPath() }
        }
        testFalsePathStrCases.forEach {
            assertTrue { !it.isMediaPath() }
        }
    }

    @Test
    fun test_illegalFileNameCharsRegex() {
        val validFileNames = listOf(
            "my_document.txt",
            "archive-2023",
            "photo.jpg",
            "presentation v2",      // Spaces are generally legal
            "ImportantFile",
            "123456789",            // Note: '0' is illegal in your regex, so we use 1-9
            "resume.pdf",
            "data_set_1",
            "notes (final)",        // Parentheses are legal
            "index.html"
        )
        val invalidFileNames = listOf(
            "file/name.txt",        // Contains /
            "C:\\Users\\Desktop",   // Contains \
            "what?is.this",         // Contains ?
            "title: subtitle",      // Contains :
            "star*file",            // Contains *
            "quote\"file",          // Contains "
            "less<than",            // Contains <
            "greater>than",         // Contains >
            "pipe|symbol",          // Contains |
        )
        validFileNames.forEach { name ->
            assertFalse(
                illegalFileNameCharsRegex.containsMatchIn(name),
                "Expected '$name' to be VALID (no illegal characters found)"
            )
        }
        invalidFileNames.forEach { name ->
            assertTrue(
                illegalFileNameCharsRegex.containsMatchIn(name),
                "Expected '$name' to be INVALID (illegal character should be detected)"
            )
        }
    }
}
