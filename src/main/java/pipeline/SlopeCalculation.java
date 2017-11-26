package pipeline;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

class SlopeCalculation
{

    private static final double ROUND_DECIMAL = 100000d;

    private static double getSlope(double stddevX, double stddevY, double corr)
    {
        return corr * (stddevY / stddevX);
    }

    private static double getCorrelation(double[] x, double[] y)
    {
        //Util.log();
        return new PearsonsCorrelation().correlation(x, y);
    }


    private static double getStandardDeviation(double[] data)
    {
        //Util.log();

        double mean = getMean(data);
        double sum = 0;

        for (double value : data)
        {
            sum += Math.pow(Math.abs(mean - value), 2);
        }

        return Math.sqrt(sum / data.length);
    }


    private static double getMean(double[] data)
    {
        //Util.log();

        double sum = 0;

        for (double aData : data)
        {
            sum += aData;
        }
        return sum / data.length;
    }


    static double getSlopeForFileMetric(double[] xCommits, double[] yMetricValues)
    {
        //Util.log();

        double stddevX = getStandardDeviation(xCommits);
        double stddevY = getStandardDeviation(yMetricValues);
        double corr = getCorrelation(xCommits, yMetricValues);
        double slope = getSlope(stddevX, stddevY, corr);
        //round slope
        slope = (double) Math.round(slope * ROUND_DECIMAL) / ROUND_DECIMAL;
        return slope;
    }

}
