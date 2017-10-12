package org.lambadaframework.runtime.router;

import org.apache.log4j.Logger;
import org.lambadaframework.jaxrs.JAXRSParser;
import org.lambadaframework.jaxrs.model.Resource;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;
import org.lambadaframework.runtime.router.types.*;

import javax.ws.rs.NotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Router class decides which method to call
 * using values in Request object
 */
public final class Router {

    static final Logger logger = Logger.getLogger(Router.class);

    private static Router singletonInstance = null;

    private Map<String, List<Resource>> resourceMap = new ConcurrentHashMap<>();

    private Map<String, ResourceMethod> routingCache = new ConcurrentHashMap<>();

    private List<RouterType> routerTypes = new LinkedList<>();

    JAXRSParser jaxrsParser;

    public Router setJaxrsParser(JAXRSParser jaxrsParser) {
        this.jaxrsParser = jaxrsParser;
        return this;
    }

    /**
     * Gets the singleton instance
     * <p>
     * Why do we use singleton? Because Handler class is directly invoked by AWS Lambda runtime
     * and we do not have a chance to inject its dependencies.
     *
     * @return Router
     */
    public static Router getRouter() {
        if (singletonInstance != null) {
            logger.debug("Singleton router is being returned.");
            return singletonInstance;
        }

        return singletonInstance = new Router();
    }

    private Router() {
        logger.debug("Router is being initialized.");
        jaxrsParser = new JAXRSParser();
        addRouterTypes();
    }

    private void addRouterTypes() {
        logger.debug("Adding router types.");
        routerTypes.add(new Path());
        routerTypes.add(new Method());
        logger.debug("Router types are added.");
    }


    /**
     * Scans package for Resources
     *
     * @param packageName Package name to scan
     * @return Found resources
     */
    protected List<Resource> getJAXRSResourcesFromPackage(String packageName) {

        logger.debug("Package is being scanned: " + packageName);

        if (null != resourceMap.get(packageName)) {
            /**
             * This package is already scanned,
             * so return cached values
             */
            logger.debug("Returning cached resource map.");
            return resourceMap.get(packageName);
        }

        logger.debug("Cached resource map not found. Scanning package.");
        JAXRSParser jaxrsParser = this.jaxrsParser.withPackageName(packageName, Router.class);
        List<Resource> foundResources = jaxrsParser.scan();
        resourceMap.put(packageName, foundResources);
        logger.debug(foundResources.size() + " resources found.");
        return foundResources;
    }


    private boolean isResourceMapMatches(RequestInterface request, ResourceMethod resourceMethod) {
        for (RouterType router : routerTypes) {
            if (!router.isMatching(request, resourceMethod)) {
                return false;
            }
        }
        return true;
    }


    private String calculateCacheKeyForRequest(RequestInterface request) {
        return request.getPathTemplate() + "-" +
                request.getMethod();
    }

    public ResourceMethod route(RequestInterface request)
            throws NotFoundException {

        if (request.getPackage() == null) {
            throw new NotFoundException("Request should have package attribute");
        }

        String cacheKey = calculateCacheKeyForRequest(request);
        ResourceMethod foundMethod;

        logger.debug("Matching request with a corresponding resource method.");


        if ((foundMethod = routingCache.get(cacheKey)) != null) {
            logger.debug("Request is already cached: " + foundMethod.getClass().toString());
            return foundMethod;
        }

        List<Resource> foundResources = getJAXRSResourcesFromPackage(request.getPackage());

        for (Resource resource : foundResources) {
            for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                if (isResourceMapMatches(request, resourceMethod)) {
                    logger.debug("Match complete: " + resourceMethod.getClass().toString());
                    routingCache.put(cacheKey, resourceMethod);
                    return resourceMethod;
                }
            }
        }

        throw new NotFoundException();
    }
}
