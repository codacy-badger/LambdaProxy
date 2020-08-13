package de.jangassen.lambda.loader;

import de.jangassen.lambda.api.RequestEvent;
import de.jangassen.lambda.lambda.LambdaProxyContext;

public class LambdaClassLoaderFactory implements ClassLoaderFactory {
    private final ArtifactResolver artifactResolver;

    public LambdaClassLoaderFactory(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public ClassLoader create(RequestEvent handler) {
        try {
            LambdaClassLoader lambdaClassLoader = new LambdaClassLoader(artifactResolver.resolve(handler));
            lambdaClassLoader.addClass(LambdaProxyContext.class);
            return lambdaClassLoader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
