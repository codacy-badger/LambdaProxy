package de.jangassen.lambda.util;

import de.jangassen.lambda.api.ApiMethod;
import de.jangassen.lambda.api.RequestEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Optional;

public final class ApiMethodUtils {

    private static final PathPatternParser pathPatternParser = new PathPatternParser();

    private ApiMethodUtils() {

    }

    public static Optional<RequestEvent> getRequestEvent(List<ApiMethod> apiMethods, String path, String method) {
        return getRequestEvent(PathContainer.parsePath(path), apiMethods, method);
    }

    private static Optional<RequestEvent> getRequestEvent(PathContainer pathContainer, List<ApiMethod> apiMethods, String method) {
        return apiMethods.stream()
                .filter(m -> StringUtils.equalsIgnoreCase(method, m.getMethod()))
                .filter(m -> pathPatternParser.parse(m.getPathPattern()).matches(pathContainer))
                .map(m -> getRequestEvent(pathContainer, m)).findFirst();
    }

    private static RequestEvent getRequestEvent(PathContainer pathContainer, ApiMethod apiMethod) {
        PathPattern.PathMatchInfo pathMatchInfo = pathPatternParser.parse(apiMethod.getPathPattern()).matchAndExtract(pathContainer);

        return new RequestEvent(apiMethod.getResourceName(), apiMethod.getCodeUri(), apiMethod.getHandler(), pathContainer.value(), apiMethod.getPathPattern(), pathMatchInfo);
    }
}
