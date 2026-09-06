package dev.piovra.model.product;

/**
 * A GPSR document (safety instructions, warning label, declaration of conformity...) attached to a
 * product. Deliberately not a {@link Media}: a marketplace's GPSR compliance API treats these as a
 * distinct concept from images (different types, no gallery/position semantics), even though both
 * share the "compare by {@code contentHash}, never by URL" rule to avoid re-uploading on CDN churn.
 */
public record ComplianceDocument(ComplianceDocumentType type, String url, String contentHash, String language) {}
