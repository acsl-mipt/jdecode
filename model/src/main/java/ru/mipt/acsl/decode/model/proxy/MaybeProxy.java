package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.registry.Registry;

/**
 * @author Artem Shein
 */
public interface MaybeProxy extends Referenceable {

    default ResolvingMessages resolve(Registry registry) {
        if (isResolved())
            return ResolvingMessages.newInstance();
        ResolvingResult<Referenceable> resolvingResult = registry.resolveElement(proxy().path());

        resolvingResult.messages().addAll(
                resolveTo(resolvingResult.result()
                        .orElseThrow(() -> new AssertionError(String.format("can't resolve proxy %s", proxy())))));
        return resolvingResult.messages();
    }

    ResolvingMessages resolveTo(Referenceable obj);

    Proxy proxy();

    boolean isResolved();

    default void accept(ReferenceableVisitor visitor) {
        visitor.visit(this);
    }

/*
  def isProxy: Boolean = v.isLeft

  def isResolved: Boolean = v.isRight

  def obj: T = v.right.getOrElse(sys.error(s"assertion error for $proxy"))

  def proxy: Proxy = v.left.getOrElse(sys.error("assertion error"))

  override def toString: String = s"MaybeProxy{${if (isProxy) proxy else obj}}"*/
}
