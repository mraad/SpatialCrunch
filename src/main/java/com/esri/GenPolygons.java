package com.esri;

import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;

import java.io.IOException;

/**
 */
public final class GenPolygons
{
    public static void main(final String args[]) throws IOException
    {
        final OperatorExportToJson exportToJson = OperatorExportToJson.local();
        final SpatialReference spatialReference = SpatialReference.create(4326);

        for (int i = 0; i < 10; i++)
        {
            final double x = -180.0 + 350.0 * Math.random();
            final double y = -90 + 170.0 * Math.random();
            final Polygon polygon = new Polygon();
            polygon.startPath(x, y);
            polygon.lineTo(x + 1.0, y);
            polygon.lineTo(x + 1.0, y + 1.0);
            polygon.lineTo(x, y + 1.0);
            polygon.lineTo(x, y);
            polygon.closeAllPaths();
            System.out.println(exportToJson.execute(spatialReference, polygon));
        }
    }
}
