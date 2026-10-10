package space.o4bit.projectasteria.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun decodeHtmlEntities_replacesEntitiesProperly() {
        val input = "Solar &amp; Heliospheric &quot;Observatory&quot; &#39;test&#39; &copy; 2026"
        val expected = "Solar & Heliospheric \"Observatory\" 'test' \u00A9 2026"
        assertEquals(expected, TextUtils.decodeHtmlEntities(input))
    }

    @Test
    fun stripHtml_removesTagsAndConvertsBreaks() {
        val input = "<p>First line.<br>Second line with <b>bold</b> text.</p>"
        val result = TextUtils.stripHtml(input)
        assertTrue(result.contains("First line.\nSecond line with bold text."))
    }

    @Test
    fun htmlToAnnotatedString_parsesApodCreditWithLinks() {
        val html = "<a href=\"https://www.nasa.gov/\">NASA</a> / <a href=\"https://www.nasa.gov/goddard/\">GSFC</a> / <a href=\"https://sdo.gsfc.nasa.gov/\">SDO</a> / AIA"
        val prefix = "Image Credit: "

        val annotated = TextUtils.htmlToAnnotatedString(
            html = html,
            prefix = prefix
        )

        assertEquals("Image Credit: NASA / GSFC / SDO / AIA", annotated.text)

        val urlAnnotations = annotated.getStringAnnotations(tag = "URL", start = 0, end = annotated.length)
        assertEquals(3, urlAnnotations.size)

        assertEquals("https://www.nasa.gov/", urlAnnotations[0].item)
        assertEquals(prefix.length, urlAnnotations[0].start)
        assertEquals(prefix.length + 4, urlAnnotations[0].end)

        assertEquals("https://www.nasa.gov/goddard/", urlAnnotations[1].item)
        assertEquals(prefix.length + 7, urlAnnotations[1].start)
        assertEquals(prefix.length + 11, urlAnnotations[1].end)

        assertEquals("https://sdo.gsfc.nasa.gov/", urlAnnotations[2].item)
        assertEquals(prefix.length + 14, urlAnnotations[2].start)
        assertEquals(prefix.length + 17, urlAnnotations[2].end)
    }

    @Test
    fun htmlToAnnotatedString_handlesPlainTextWithoutLinks() {
        val plain = "Robert Nemiroff &amp; Jerry Bonnell"
        val prefix = "Image Credit: "

        val annotated = TextUtils.htmlToAnnotatedString(
            html = plain,
            prefix = prefix
        )

        assertEquals("Image Credit: Robert Nemiroff & Jerry Bonnell", annotated.text)
        val urlAnnotations = annotated.getStringAnnotations(tag = "URL", start = 0, end = annotated.length)
        assertTrue(urlAnnotations.isEmpty())
    }

    @Test
    fun htmlToAnnotatedString_handlesBlankInput() {
        val annotated = TextUtils.htmlToAnnotatedString(
            html = "   ",
            prefix = "Image Credit: "
        )
        assertEquals("Image Credit: ", annotated.text)
    }
}
