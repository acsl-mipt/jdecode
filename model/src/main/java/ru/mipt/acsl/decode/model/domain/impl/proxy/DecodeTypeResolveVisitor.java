package ru.mipt.acsl.decode.model.domain.impl.proxy;

import org.jetbrains.annotations.NotNull;
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
    private final Registry registry;
    @NotNull
    private final Buffer<ResolvingResult<Referenceable>> resolvingResultList;

    public DecodeTypeResolveVisitor(@NotNull Registry registry,
                                    @NotNull Buffer<ResolvingResult<Referenceable>> resolvingResultList)
    {
        this.registry = registry;
        this.resolvingResultList = resolvingResultList;
    }

    @Override
    public Void visit(@NotNull PrimitiveType baseType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull NativeType nativeType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull SubType subType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(subType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull EnumType enumType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(enumType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull ArrayType arrayType)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(arrayType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull StructType structType)
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
                        DecodeModelResolver.resolveWithTypeCheck(typeUnit.unit().get(), registry, Measure.class));
            }
        }
        return null;
    }

    @Override
    public Void visit(@NotNull AliasType typeAlias)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver.resolveWithTypeCheck(typeAlias.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull GenericType genericType)
    {
        return null;
    }

    @Override
    public Void visit(@NotNull GenericTypeSpecialized genericTypeSpecialized)
    {
        appendToBuffer(resolvingResultList,
                DecodeModelResolver
                        .resolveWithTypeCheck(genericTypeSpecialized.genericType(), registry, GenericType.class));
        Iterator<Option<MaybeProxy<DecodeType>>> taIt = genericTypeSpecialized.genericTypeArguments().iterator();
        while (taIt.hasNext())
        {
            Option<MaybeProxy<DecodeType>> ta = taIt.next();
            if (ta.isDefined())
                appendToBuffer(resolvingResultList,
                                DecodeModelResolver.resolveWithTypeCheck(ta.get(), registry, DecodeType.class));

        }
        return null;
    }
}
