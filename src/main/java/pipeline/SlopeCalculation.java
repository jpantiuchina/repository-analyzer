package pipeline;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 *
 */

public class SlopeCalculation
{

    public static double stddevX;
    public static double stddevY;
    public static double corr;
    public static double slope;

    public static double getSlope(double stddevX, double stddevY, double corr)
    {
        return corr*(stddevY/stddevX);
    }

    public static double getCorrelation(double[] x, double[] y)
    {
        return new PearsonsCorrelation().correlation(x, y);
    }


    public static double getStandardDeviation(double[] data) {
        final double mean = getMean(data);
        double sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += Math.pow(Math.abs(mean - data[i]), 2);
        }
        return Math.sqrt(sum / data.length);
    }


    public static double getMean(double[] data) {
        double sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum / data.length;
    }


    public static double getSlopeForFileMetric(double[] Xcommits, double[] YmetricValues)
    {
        stddevX = getStandardDeviation(Xcommits);
        stddevY = getStandardDeviation(YmetricValues);
        corr = getCorrelation(Xcommits, YmetricValues);
        slope = getSlope(stddevX,stddevY,corr);

        slope = (double)Math.round(slope * 100000d) / 100000d;
        return slope;
    }

}
