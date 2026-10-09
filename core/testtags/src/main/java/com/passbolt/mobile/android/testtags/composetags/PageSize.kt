package net.svaroh.passly.testtags.composetags

object PageSize {
    // the slider carries no text; it is driven through the SetProgress semantics action
    const val SLIDER = "PageSizeSlider"

    // the selected value duplicates the slider range labels at both ends ("250" / "10,000"),
    // so matching the headline by text is ambiguous
    const val HEADLINE = "PageSizeHeadline"
}
