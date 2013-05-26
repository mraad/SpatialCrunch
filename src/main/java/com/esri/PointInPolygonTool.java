package com.esri;

import org.apache.crunch.PTable;
import org.apache.crunch.Pipeline;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;


public final class PointInPolygonTool extends Configured implements Tool
{
    public static void main(final String[] args) throws Exception
    {
        System.exit(ToolRunner.run(new Configuration(), new PointInPolygonTool(), args));
    }

    public int run(final String[] args) throws Exception
    {
        if (args.length != 2)
        {
            System.err.println("Usage: hadoop jar CrunchApp-1.0-SNAPSHOT-job.jar"
                    + " [generic options] -files <polygons.json> <input> <output>");
            System.err.println();
            GenericOptionsParser.printGenericCommandUsage(System.err);
            return 1;
        }

        final Pipeline pipeline = new MRPipeline(PointInPolygonTool.class, getConf());

        final PTable<Long, Long> counts = pipeline.
                readTextFile(args[0]).
                parallelDo(new PointInPolygon(), Writables.longs()).
                count();

        pipeline.writeTextFile(counts, args[1]);

        final PipelineResult result = pipeline.done();

        return result.succeeded() ? 0 : 1;
    }
}
