package com.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.Point;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class PointInPolygon extends DoFn<String, Long>
{
    private static final long serialVersionUID = 2686105881780043617L;

    public static final String POINT_IN_POLYGON_FILE = "pointInPolygon.file";

    public static enum COUNTERS
    {
        NumberFormatException,
        IOException
    }

    private transient int m_lat;
    private transient int m_lon;
    private transient Pattern m_pattern;
    private transient Point m_point;
    private transient List<MapGeometry> m_geometryList;

    @Override
    public void initialize()
    {
        super.initialize();
        final Configuration configuration = getConfiguration();
        m_point = new Point();
        m_geometryList = new ArrayList<MapGeometry>();
        m_lat = configuration.getInt("pointInPolygon.lat", 1);
        m_lon = configuration.getInt("pointInPolygon.lon", 2);
        m_pattern = Pattern.compile("\\t");
        try
        {
            final Path[] paths = DistributedCache.getLocalCacheFiles(configuration);
            if (paths != null && paths.length > 0)
            {
                final String file = configuration.get(POINT_IN_POLYGON_FILE, "polygons.json");
                for (final Path path : paths)
                {
                    if (path.getName().equals(file))
                    {
                        final Reader reader = new InputStreamReader(new FileInputStream(path.toString()), Charset.forName("UTF-8"));
                        try
                        {
                            final OperatorImportFromJson operatorImportFromJson = OperatorImportFromJson.local();
                            final LineNumberReader lineNumberReader = new LineNumberReader(reader);
                            String line = lineNumberReader.readLine();
                            while (line != null)
                            {
                                m_geometryList.add(operatorImportFromJson.execute(Geometry.Type.Polygon, line));
                                line = lineNumberReader.readLine();
                            }
                        }
                        finally
                        {
                            reader.close();
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            getCounter(COUNTERS.IOException).increment(1);
        }
    }

    @Override
    public void process(
            final String line,
            final Emitter<Long> emitter)
    {
        try
        {
            final String[] tokens = m_pattern.split(line);
            m_point.setY(Double.parseDouble(tokens[m_lat]));
            m_point.setX(Double.parseDouble(tokens[m_lon]));
            long index = 0;
            // Brute force search since the number of polygons is small - a better way will be to use a spatial index !
            for (final MapGeometry mapGeometry : m_geometryList)
            {
                if (GeometryEngine.contains(mapGeometry.getGeometry(), m_point, mapGeometry.getSpatialReference()))
                {
                    emitter.emit(index);
                    break;
                }
                index++;
            }
        }
        catch (NumberFormatException e)
        {
            getCounter(COUNTERS.NumberFormatException).increment(1);
        }
    }
}
