package io.antmedia.statistic;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;

import io.antmedia.IResourceMonitor;
import io.antmedia.SystemUtils;
import io.vertx.core.Vertx;

public class ResourceMonitor implements IResourceMonitor{
	
	@Autowired
	private Vertx vertx;
	private Queue<Integer> cpuMeasurements = new ConcurrentLinkedQueue<>();

	private int windowSize = 5;
	private int measurementPeriod = 3000;
	private int avgCpuUsage;
	private int cpuLimit = 70;
	
	public void start() {
		getVertx().setPeriodic(measurementPeriod, l -> {
			addCpuMeasurement(SystemUtils.getSystemCpuLoad());
		});
	}
	
	
	public void addCpuMeasurement(int measurment) {
		cpuMeasurements.add(measurment);
		if(cpuMeasurements.size() > windowSize) {
			cpuMeasurements.poll();
		}
		
		int total = 0;
		for (int msrmnt : cpuMeasurements) {
			total += msrmnt;
		}		
		avgCpuUsage = total/cpuMeasurements.size();
	}


	@Override
	public int getAvgCpuUsage() {
		return avgCpuUsage;
	}


	public int getWindowSize() {
		return windowSize;
	}


	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}


	public Vertx getVertx() {
		return vertx;
	}


	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}


	public void setCpuLimit(int cpuLimit) {
		this.cpuLimit = cpuLimit;
	}
	
	@Override
	public int getCpuLimit() {
		return cpuLimit;
	}
}
