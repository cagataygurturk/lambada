package org.lambadaframework.runtime.router.types;


import org.glassfish.jersey.uri.UriTemplate;
import org.lambadaframework.jaxrs.model.Resource;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;

import java.util.HashMap;

public class Path implements RouterType {

    @Override
    public boolean isMatching(RequestInterface request, ResourceMethod resourceMethod) {
        Resource resource = resourceMethod.getParent();

        try {
            return new UriTemplate(resource.getPath()).match(request.getPathTemplate(), new HashMap<>());
        } catch (NullPointerException e) {
            return false;
        }
    }
}
