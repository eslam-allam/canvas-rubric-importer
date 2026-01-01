package io.github.eslam_allam.canvas.domain;

import java.net.URISyntaxException;
import org.apache.hc.core5.net.URIBuilder;

public final class DynamicURIBuilder {
    private final URIProvider provider;
    private final URIBuilder uri;

    private DynamicURIBuilder(URIProvider provider) {
        this.provider = provider;
        this.uri = new URIBuilder();
    }

    public static DynamicURIBuilder of(URIProvider provider) {
        return new DynamicURIBuilder(provider);
    }

    public DynamicURIBuilder appendPath(String path) {
        this.uri.appendPath(path);
        return this;
    }

    public DynamicURIBuilder appendPathSegments(String... pathSegments) {
        this.uri.appendPathSegments(pathSegments);
        return this;
    }

    public URIBuilder newInstance() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(this.provider.getUri());
        builder.setFragment(this.uri.getFragment());
        builder.setPath(this.uri.getPath());
        builder.addParameters(this.uri.getQueryParams());
        return builder;
    }
}
