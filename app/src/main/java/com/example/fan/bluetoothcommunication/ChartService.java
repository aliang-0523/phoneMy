package com.example.fan.bluetoothcommunication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.List;

public class ChartService {

    private static final double Max_Point=100;
	private GraphicalView mGraphicalView;  //图表控件
	private XYMultipleSeriesDataset multipleSeriesDataset;//数据集容器
	private XYMultipleSeriesRenderer multipleSeriesRenderer;// 渲染器容器
	private XYSeries mSeries;// 单条线性数据集
	private XYSeriesRenderer mRenderer;// 单条曲线渲染器
	private Context context;

	public ChartService(Context context) {
		this.context = context;
	}


	/**
	 * 获取图表
	 *
	 * @return
	 */
	public GraphicalView getGraphicalView() {
		mGraphicalView = ChartFactory.getCubeLineChartView(context,
				multipleSeriesDataset, multipleSeriesRenderer, 0.1f);
		return mGraphicalView;
	}

	/**
	 * 设置数据集容器，及xy坐标的集合
	 *
	 * @param curveTitle
	 *
	 */
	public void setXYMultipleSeriesDataset(String curveTitle) {
		multipleSeriesDataset = new XYMultipleSeriesDataset();
		mSeries = new XYSeries(curveTitle);
		multipleSeriesDataset.addSeries(mSeries);
	}


	/**
	 * 设置渲染器
	 *
	 * @param maxX
	 *            x轴最大值
	 * @param maxY
	 *            y轴最大值
	 * @param chartTitle
	 *            表格的标题
	 * @param xTitle
	 *            x轴标题
	 * @param yTitle
	 *            y轴标题
	 * @param axeColor
	 *            坐标轴颜色
	 * @param labelColor
	 *            标题颜色
	 * @param curveColor
	 *            曲线颜色
	 * @param gridColor
	 *            网格颜色
	 */
	public void setXYMultipleSeriesRenderer(double maxX, double maxY,
			String chartTitle, String xTitle, String yTitle, int axeColor,
			int labelColor, int curveColor, int gridColor) {
		multipleSeriesRenderer = new XYMultipleSeriesRenderer();
		if (chartTitle != null) {
			multipleSeriesRenderer.setChartTitle(chartTitle); //给图表加标题
		}
		multipleSeriesRenderer.setXTitle(xTitle);  //x轴标题
		multipleSeriesRenderer.setYTitle(yTitle);  //y轴标题
		multipleSeriesRenderer.setRange(new double[]{0, maxX, 0, maxY});// xy轴的范围
		multipleSeriesRenderer.setLabelsColor(labelColor); //标题颜色
		multipleSeriesRenderer.setXLabels(20); // 将x轴等分为20个点
		multipleSeriesRenderer.setYLabels(20);  // 将y轴等分为20个点
		multipleSeriesRenderer.setXLabelsAlign(Align.RIGHT);
		multipleSeriesRenderer.setYLabelsAlign(Align.RIGHT);
		multipleSeriesRenderer.setAxisTitleTextSize(20);
		multipleSeriesRenderer.setChartTitleTextSize(20);
		multipleSeriesRenderer.setLabelsTextSize(20);
		multipleSeriesRenderer.setLegendTextSize(20);
		multipleSeriesRenderer.setPointSize(2f);// 曲线描点尺寸
		multipleSeriesRenderer.setFitLegend(true);
		multipleSeriesRenderer.setMargins(new int[] { 20, 30, 15, 20 });
		multipleSeriesRenderer.setShowGrid(true);
		multipleSeriesRenderer.setZoomEnabled(true, false);
		multipleSeriesRenderer.setAxesColor(axeColor);  //坐标轴颜色
		multipleSeriesRenderer.setGridColor(gridColor); //网格颜色
		multipleSeriesRenderer.setBackgroundColor(Color.WHITE);// 背景色
		multipleSeriesRenderer.setMarginsColor(Color.WHITE);// 边距背景色，默认背景色为黑色，这里修改为白色
		mRenderer = new XYSeriesRenderer();
		mRenderer.setColor(curveColor);
		mRenderer.setPointStyle(PointStyle.CIRCLE);// 描点风格，可以为圆点，方形点等等
		multipleSeriesRenderer.addSeriesRenderer(mRenderer);
		multipleSeriesRenderer.setZoomEnabled(false,false);
	}

	/**
	 * 根据新加的数据，更新曲线，只能运行在主线程
	 *
	 * @param x
	 *            新加点的x坐标
	 * @param y
	 *            新加点的y坐标
	 */
	public void updateChart(double x, double y) {
        mSeries.add(x, y);
        if (x < Max_Point) {
            multipleSeriesRenderer.setXAxisMin(0);
            multipleSeriesRenderer.setXAxisMax(Max_Point);
        } else {
            multipleSeriesRenderer.setXAxisMin(mSeries.getItemCount()*2 - Max_Point);
            multipleSeriesRenderer.setXAxisMax(mSeries.getItemCount()*2);
        }

        mGraphicalView.repaint();
		//mGraphicalView.repaint();
		//mGraphicalView.invalidate();
	}

	/**
	 * 添加新的数据，多组，更新曲线，只能运行在主线程
	 *
	 * @param xList
	 * @param yList
	 */
	public void updateChart(List<Double> xList, List<Double> yList) {
		for (int i = 0; i < xList.size(); i++) {
			mSeries.add(xList.get(i), yList.get(i));
		}
		mGraphicalView.repaint();
	}

}
