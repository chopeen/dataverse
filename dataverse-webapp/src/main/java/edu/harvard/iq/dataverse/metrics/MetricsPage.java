package edu.harvard.iq.dataverse.metrics;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.chart.BarChartModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("MetricsPage")
public class MetricsPage implements Serializable {

    private ChartCreator chartCreator;
    private MetricsServiceBean metricsService;

    private final String CHART_TYPE = "publishedDatasets";

    private BarChartModel publishedDatasetsModel;
    private List<ChartMetrics> publishedDatasetsYearlyStats = new ArrayList<>();
    private List<ChartMetrics> datasetsMetrics = new ArrayList<>();

    private String mode = "YEAR";
    private int selectedYear;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public MetricsPage() {
    }

    @Inject
    public MetricsPage(ChartCreator chartCreator, MetricsServiceBean metricsService) {
        this.chartCreator = chartCreator;
        this.metricsService = metricsService;
    }

    // -------------------- GETTERS --------------------
    public BarChartModel getPublishedDatasetsModel() {
        return publishedDatasetsModel;
    }

    public List<ChartMetrics> getPublishedDatasetsYearlyStats() {
        return publishedDatasetsYearlyStats;
    }

    public String getMode() {
        return mode;
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    // -------------------- LOGIC --------------------
    public void init() {
        datasetsMetrics = metricsService.countPublishedDatasets();

        if (datasetsMetrics.isEmpty()) {
            publishedDatasetsYearlyStats.add(new ChartMetrics((double) LocalDateTime.now().getYear(), 0L));
            selectedYear = LocalDate.now().getYear();
        } else {
            publishedDatasetsYearlyStats = MetricsUtil.countMetricsPerYearAndFillMissingYearsDescending(datasetsMetrics);
            selectedYear = publishedDatasetsYearlyStats.get(0).getYear();
        }

        publishedDatasetsModel = chartCreator.createYearlyChart(metricsService.countPublishedDatasets(), CHART_TYPE, mode);
    }

    public void changeDatasetMetricsModel() {
        if (shouldGenerateYearlyModel()) {
            publishedDatasetsModel = chartCreator.createYearlyChart(datasetsMetrics, CHART_TYPE, mode);

        } else if (shouldGenerateMonthlyModel()) {
            publishedDatasetsModel = chartCreator.createMonthlyChart(datasetsMetrics, selectedYear, CHART_TYPE, mode);
        }
    }

    // -------------------- PRIVATE ---------------------
    private boolean shouldGenerateMonthlyModel() {
        return selectedYear != 0;
    }

    private boolean shouldGenerateYearlyModel() {
        return mode.equals("YEAR");
    }

    // -------------------- SETTERS --------------------
    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

    public void setPublishedDatasetsYearlyStats(List<ChartMetrics> publishedDatasetsYearlyStats) {
        this.publishedDatasetsYearlyStats = publishedDatasetsYearlyStats;
    }
}
