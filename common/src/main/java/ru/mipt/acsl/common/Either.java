package ru.mipt.acsl.common;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public abstract class Either<L, R>
{
    public static <L, R> Left<L, R> left(@NotNull L left)
    {
        return new Left<>(left);
    }

    public static <L, R> Right<L, R> right(@NotNull R right)
    {
        return new Right<>(right);
    }

    public abstract boolean isLeft();
    public abstract boolean isRight();
    public abstract L getLeft();
    public abstract R getRight();

    private static final class Left<L, R> extends Either<L, R>
    {
        @NotNull
        private final L value;

        public Left(@NotNull L left)
        {
            this.value = left;
        }

        @Override
        public boolean isLeft()
        {
            return true;
        }

        @Override
        public boolean isRight()
        {
            return false;
        }

        @Override
        public L getLeft()
        {
            return value;
        }

        @Override
        public R getRight()
        {
            throw new AssertionError();
        }
    }

    private static final class Right<L, R> extends Either<L, R>
    {
        @NotNull
        private final R value;

        public Right(@NotNull R right)
        {
            this.value = right;
        }

        @Override
        public boolean isLeft()
        {
            return false;
        }

        @Override
        public boolean isRight()
        {
            return true;
        }

        @Override
        public L getLeft()
        {
            throw new AssertionError();
        }

        @Override
        public R getRight()
        {
            return value;
        }
    }
}
