package com.esri;

import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import org.apache.commons.io.IOUtils;
import org.apache.crunch.Emitter;
import org.apache.crunch.test.CrunchTestSupport;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class PointInPolygonTest
{
    @Mock
    private Emitter<Long> m_emitter;

    private File m_file;
    private Configuration m_configuration;

    @Before
    public void setUp() throws Exception
    {
        final SpatialReference spatialReference = SpatialReference.create(4326);
        final List<String> lines = new ArrayList<String>();

        final Polygon polygon = new Polygon();
        polygon.startPath(0.0, 0.0);
        polygon.lineTo(10.0, 0.0);
        polygon.lineTo(10.0, 10.0);
        polygon.lineTo(0.0, 10.0);
        polygon.lineTo(0.0, 0.0);
        polygon.closeAllPaths();

        lines.add(OperatorExportToJson.local().execute(spatialReference, polygon));

        m_file = File.createTempFile("polygons", "json");
        m_file.deleteOnExit();
        final OutputStream outputStream = new FileOutputStream(m_file);
        try
        {
            IOUtils.writeLines(lines, "\n", outputStream, Charset.forName("UTF-8"));
        }
        finally
        {
            outputStream.close();
        }

        m_configuration = new Configuration(false);
        m_configuration.set(PointInPolygon.POINT_IN_POLYGON_FILE, m_file.getName());
        DistributedCache.addLocalFiles(m_configuration, m_file.getPath());
    }

    @Test
    public void testProcess() throws Exception
    {
        final PointInPolygon pointInPolygon = new PointInPolygon();
        pointInPolygon.setContext(CrunchTestSupport.getTestContext(m_configuration));
        pointInPolygon.initialize();
        pointInPolygon.process("FOO\t5.0\t5.0", m_emitter);
        verify(m_emitter).emit(0L);
        verifyNoMoreInteractions(m_emitter);
    }

}
