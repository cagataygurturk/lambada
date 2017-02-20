package org.lambadaframework.runtime.router.types;


import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;

public class ProducedTypes implements RouterType {
    @Override
    public boolean isMatching(RequestInterface request, ResourceMethod resourceMethod) {
        return resourceMethod.getProducedTypes().contains(request.getProducedMediaType());
    }
}
