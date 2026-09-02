package dev.piovra.publication.domain;

public enum ListingState {
    /** Never published on this channel. */
    NOT_LISTED,
    /** Command emitted, outcome not received yet. */
    PENDING,
    LISTED,
    /** Permanent error: someone has to intervene. */
    ERROR,
    /** Withdrawn from the channel. */
    ENDED,
    /** The listing rules are not satisfied: we do not even attempt to publish. */
    BLOCKED
}
