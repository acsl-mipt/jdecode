package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath;
import ru.mipt.acsl.decode.model.registry.Registry;

/**
 * @author Artem Shein
 */
public interface DecodeProxyResolver {

    ResolvingResult<Referenceable> resolveElement(Registry registry, ProxyPath path);

}
