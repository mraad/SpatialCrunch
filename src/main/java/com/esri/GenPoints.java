package com.esri;

/**
 */
public final class GenPoints
{
    public static void main(final String[] args)
    {
        final int count = args.length == 0 ? 1000000 : Integer.parseInt(args[0]);
        for (int i = 0; i < count; i++)
        {
            final double x = -180 + 360 * Math.random();
            final double y = -90 + 180 * Math.random();
            System.out.format("L%d\t%.6f\t%.6f\n", i, y, x);
        }
    }
}
