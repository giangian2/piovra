package dev.piovra.model.product;

/**
 * A product image. {@code contentHash} is what enters the diff: comparing URLs re-uploads the same
 * images every time the feed changes CDN, and image upload is the slowest call on any marketplace.
 */
public record Media(MediaRole role, String url, int position, String contentHash) {

    public static Media main(String url) {
        return new Media(MediaRole.MAIN, url, 0, null);
    }
}
