package de.jangassen.lambda.api;

import de.jangassen.lambda.OpenApiParser;
import de.jangassen.lambda.util.EventUtils;
import de.jangassen.lambda.util.ParameterUtils;
import de.jangassen.lambda.util.ResourceUtils;
import de.jangassen.lambda.yaml.SamTemplate;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SamApiDescription implements ApiDescription {

    private static final String FN_TRANSFORM = "Fn::Transform";
    private static final String FN_SUB = "Fn::Sub";

    private static final String AWS_INCLUDE = "AWS::Include";

    private static final String NAME = "Name";
    private static final String PARAMETERS = "Parameters";
    private static final String LOCATION = "Location";

    private final SamTemplate samTemplate;
    private final Path projectPath;

    public SamApiDescription(SamTemplate samTemplate, Path projectPath) {
        this.samTemplate = samTemplate;
        this.projectPath = projectPath;
    }

    @Override
    public List<ApiMethod> getApiMethods() {
        return getApiMethods(samTemplate);
    }

    private List<ApiMethod> getApiMethods(SamTemplate samTemplate) {
        return samTemplate.Resources.entrySet()
                .stream()
                .flatMap(r -> getApiMethods(r.getValue(), r.getKey()))
                .collect(Collectors.toList());
    }

    private Stream<ApiMethod> getApiMethods(SamTemplate.Resource resource, String resourceName) {
        if (resource.Properties.Events == null && resource.Properties.DefinitionBody != null) {
            return getOpenApiMethods(resource).stream();
        } else if (resource.Properties.Events != null && ResourceUtils.isJava8Runtime(resource)) {
            return getTemplateMethods(resourceName, resource);
        } else {
            return Stream.empty();
        }
    }

    private Stream<ApiMethod> getTemplateMethods(String resourceName, SamTemplate.Resource resource) {
        return resource.Properties.Events.values()
                .stream()
                .filter(EventUtils::isAPIEvent)
                .map(event -> getApiMethod(resourceName, resource, event));
    }

    private ApiMethod getApiMethod(String resourceName, SamTemplate.Resource resource, SamTemplate.Event event) {
        return new ApiMethod(resourceName, resource.Properties.CodeUri, resource.Properties.Handler, event.Properties.Path, event.Properties.Method);
    }

    private List<ApiMethod> getOpenApiMethods(SamTemplate.Resource resource) {
        if (ObjectUtils.isEmpty(resource.Properties.Events)) {
            if (!ObjectUtils.isEmpty(resource.Properties.DefinitionBody)) {
                Object fnTransform = resource.Properties.DefinitionBody.get(FN_TRANSFORM);
                if (fnTransform instanceof Map) {
                    String name = (String) ((Map<?, ?>) fnTransform).get(NAME);
                    Object parameters = ((Map<?, ?>) fnTransform).get(PARAMETERS);
                    if (AWS_INCLUDE.equals(name) && parameters instanceof Map) {
                        String location = getLocation(((Map<?, ?>) parameters).get(LOCATION));
                        return getOpenApiMethods(location);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private List<ApiMethod> getOpenApiMethods(String location) {
        Path absolutePath = (location.startsWith("/"))
                ? new File(location).toPath()
                : projectPath.resolve(location);

        OpenAPI openAPI = new OpenApiParser().parse(absolutePath);
        OpenApiDescription openApiDescription = new OpenApiDescription(openAPI, samTemplate);
        return openApiDescription.getApiMethods();
    }

    private String getLocation(Object location) {
        if (location instanceof Map) {
            Object templatedLocationString = ((Map<?, ?>) location).get(FN_SUB);
            if (templatedLocationString instanceof String) {
                return ParameterUtils.resolve((String) templatedLocationString, samTemplate);
            }
        }
        return String.valueOf(location);
    }
}
