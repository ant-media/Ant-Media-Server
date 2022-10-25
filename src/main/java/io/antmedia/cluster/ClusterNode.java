package io.antmedia.cluster;

import java.io.Serializable;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.NotSaved;

@Entity("clusternode")
@Indexes({ @Index(fields = @Field("id"))})
public class ClusterNode implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public static final String ALIVE = "alive";
	public static final String DEAD = "dead";
	public static final long NODE_UPDATE_PERIOD = 5000;
	
	@Id
	private String id;
	private String ip;
	private long lastUpdateTime;
	private String memory;
	private String cpu;
	
	@NotSaved
	private String status;
	
	public ClusterNode() {
	}
	
	public ClusterNode(String ip, String id) {
		super();
		this.ip = ip;
		this.id = id;
		this.lastUpdateTime= System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		if(System.currentTimeMillis() - lastUpdateTime > NODE_UPDATE_PERIOD*2) {
			status = ClusterNode.DEAD;
		}
		else {
			status = ClusterNode.ALIVE;
		}
		return status;
	}
	

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	public String getCpu() {
		return cpu;
	}

	public void setCpu(String cpu) {
		this.cpu = cpu;
	}
}
