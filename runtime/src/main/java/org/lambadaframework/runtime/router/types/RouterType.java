package org.lambadaframework.runtime.router.types;


import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;

public interface RouterType {

    boolean isMatching(RequestInterface request, ResourceMethod resourceMethod);
}
