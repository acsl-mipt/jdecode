package ru.mipt.acsl.decode.model.domain.impl.proxy;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.ScalaUtil;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.DecodeModelResolver;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.mutable.Buffer;

import static ru.mipt.acsl.decode.ScalaUtil.appendToBuffer;


/**
 * @author Artem Shein
 */
public class DecodeTypeResolveVisitor implements DecodeTypeVisitor<Void>
{
    @NotNull
    private final DecodeRegistry registry;
    @NotNull
    private final Buffer<DecodeResolvingResult<DecodeReferenceable>> resolvingResultList;

    public DecodeTypeResolveVisitor(@NotNull DecodeRegistry registry,
                                    @NotNull Buffer<DecodeResolvingResult<DecodeReferenceable>> resolvingResultList)
    {
        this.registry = registry;
        this.resolvingResultList = resolvingResultList;
    }

    @Override
    public Void visit(@NotNull DecodePrimitiveType baseType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeNativeType nativeType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeSubType subType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(subType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeEnumType enumType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(enumType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeArrayType arrayType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(arrayType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeStructType structType)
    {
        Iterator<DecodeStructField> iterator = structType.fields().iterator();
        while (iterator.hasNext())
        {
            DecodeStructField field = iterator.next();
            DecodeTypeUnitApplication typeUnit = field.typeUnit();
            appendToBuffer(resolvingResultList,
                    DecodeModelResolver.resolveWithTypeCheck(typeUnit.t(), registry, DecodeType.class));
            if (!typeUnit.t().isProxy())
            {
                appendToBuffer(resolvingResultList, DecodeModelResolver.resolve(typeUnit.t().obj(), registry));
            }
            if (typeUnit.unit().isDefined())
            {
                appendToBuffer(resolvingResultList,
                        DecodeModelResolver.resolveWithTypeCheck(typeUnit.unit().get(), registry, DecodeUnit.class));
            }
        }
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeAliasType typeAlias)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(typeAlias.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeGenericType genericType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeGenericTypeSpecialized genericTypeSpecialized)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver
                        .resolveWithTypeCheck(genericTypeSpecialized.genericType(), registry, DecodeGenericType.class));
        Iterator<Option<DecodeMaybeProxy<DecodeType>>> taIt = genericTypeSpecialized.genericTypeArguments().iterator();
        while (taIt.hasNext())
        {
            Option<DecodeMaybeProxy<DecodeType>> ta = taIt.next();
            if (ta.isDefined())
                appendToBuffer(resolvingResultList,
                                DecodeModelResolver.resolveWithTypeCheck(ta.get(), registry, DecodeType.class));

        }
        return null;
    }
}
