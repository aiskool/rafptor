# ICC profile

PDF/A-1b conformance requires an output intent with an embedded ICC profile. Place `sRGB_IEC61966-2-1.icc` in this directory.

Download source: <https://www.color.org/srgbprofiles.xalter>.

The `.icc` is not checked into git. When absent, `PdfACompliance` logs a clear warning and the generated PDF, while still readable, is not fully PDF/A-1b compliant.
