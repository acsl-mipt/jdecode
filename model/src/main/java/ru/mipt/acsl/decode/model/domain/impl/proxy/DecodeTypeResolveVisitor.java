package ru.mipt.acsl.decode.model.domain.impl.proxy;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.DecodeModelResolver;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.mutable.Buffer;

import java.util.List;

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
        resolvingResultList.$plus$eq(
                DecodeModelResolver.resolveWithTypeCheck(subType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeEnumType enumType)
    {
        resolvingResultList.$plus$eq(
                DecodeModelResolver.resolveWithTypeCheck(enumType.baseType(), registry, DecodeType.class));
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeArrayType arrayType)
    {
        resolvingResultList.$plus$eq(
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
            resolvingResultList.$plus$eq(
                    DecodeModelResolver.resolveWithTypeCheck(field.fieldType(), registry, DecodeType.class));
            if (!field.fieldType().isProxy())
            {
                resolvingResultList.$plus$eq(DecodeModelResolver.resolve(field.fieldType().obj(), registry));
            }
            if (field.unit().isDefined())
            {
                resolvingResultList.$plus$eq(
                        DecodeModelResolver.resolveWithTypeCheck(field.unit().get(), registry, DecodeUnit.class));
            }
        }
        return null;
    }

    @Override
    public Void visit(@NotNull DecodeAliasType typeAlias)
    {
        resolvingResultList.$plus$eq(
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
        resolvingResultList
                .$plus$eq(DecodeModelResolver
                        .resolveWithTypeCheck(genericTypeSpecialized.genericType(), registry, DecodeGenericType.class));
        genericTypeSpecialized.genericTypeArguments().filter(Option::isDefined).foreach(
                arg -> resolvingResultList.$plus$eq(
                        DecodeModelResolver.resolveWithTypeCheck(arg.get(), registry, DecodeType.class)));
        return null;
    }
}
