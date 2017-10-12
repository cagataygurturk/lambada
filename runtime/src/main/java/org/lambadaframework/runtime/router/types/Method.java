package org.lambadaframework.runtime.router.types;


import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;

public class Method implements RouterType {

    @Override
    public boolean isMatching(RequestInterface request, ResourceMethod resourceMethod) {
        return resourceMethod.getHttpMethod().equals(request.getMethod().name());
    }
}
