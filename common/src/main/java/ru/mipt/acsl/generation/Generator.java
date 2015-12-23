package ru.mipt.acsl.generation;

/**
 * @author Artem Shein
 */
public interface Generator<C>
{
    /**
     * @throws GenerationException
     */
    void generate();

    C getConfiguration();
}
